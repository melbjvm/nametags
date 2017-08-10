package io.github.linead.nametags;

import io.github.linead.nametags.domain.Event;
import io.github.linead.nametags.domain.Members;
import io.github.linead.nametags.domain.Rsvps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MeetupData {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${meetup-api.key}")
    private String key;

    @Value("${meetup-url-path}")
    private String meetupPath;

    @Autowired
    private RestTemplate restTemplate;

    private static final String EVENT_URL = "https://api.meetup.com/{meetup_url_path}/events?scroll=recent_past&photo-host=public&page=20&key={key}";

    private static final String RSVP_URL = "https://api.meetup.com/2/rsvps?&sign=true&key={key}&event_id={event_id}&photo-host=public&page=120&rsvp=yes";

    private static final String MEMBERS_URL = "https://api.meetup.com/2/members?&key={key}&sign=true&photo-host=public&group_id={group_id}&page=100&only=id,joined,photo&offset={page}";

    private static final String HOSTS_URL = "https://api.meetup.com/{meetup_url_path}/events/{event_id}/hosts?key={key}";

    public String getKey() { return key; }

    public Event[] getNextMeetups() {

        Map<String, Object> params = new HashMap<>();
        params.put("key", getKey());
        params.put("meetup_url_path", getMeetupPath());

        log.info("getNextMeetups() querying URL:" + EVENT_URL);

        ResponseEntity<Event[]> responseEntity = restTemplate
          .getForEntity(EVENT_URL, Event[].class, params);
        Event[] events = responseEntity.getBody();

        return events;

    }

    public Rsvps getRsvps(String eventId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("event_id", eventId);
        params.put("key", getKey());

        log.info("getRsvps() querying URL:" + RSVP_URL);

        ResponseEntity<Rsvps> responseEntity = restTemplate.getForEntity(RSVP_URL, Rsvps.class, params);
        Rsvps rsvps = responseEntity.getBody();
        
        return rsvps;
    }

    @Cacheable("members")
    public Map<String, Members.Member> getMembers(String groupId) {
        Map<String, Object> params = new HashMap<String, Object>();
        int page = 0;
        params.put("group_id", groupId);
        params.put("key", getKey());
        params.put("page", page);

        log.info("getMembers() querying page " + page + " URL:" + MEMBERS_URL);
        ResponseEntity<Members> responseEntity = restTemplate.getForEntity(MEMBERS_URL, Members.class, params);
        Members members = responseEntity.getBody();

        HashMap<String, Members.Member> memberMap = new HashMap<String, Members.Member>();

        memberMap.putAll(Arrays.asList(members.getResults())
                .stream().filter(s -> s.getId() != null)
                .collect(
                        Collectors.toMap(Members.Member::getId, Function.identity()
                        )));

        //read all pages
        while(!StringUtils.isEmpty(members.getMeta().getNext())) {
            params.put("page", ++page);

            log.info("getMembers() querying page " + page + " URL:" + MEMBERS_URL);
            responseEntity = restTemplate.getForEntity(MEMBERS_URL, Members.class, params);
            members = responseEntity.getBody();

            memberMap.putAll(Arrays.asList(members.getResults())
                    .stream().filter(s -> s.getId() != null)
                    .collect(
                            Collectors.toMap(Members.Member::getId, Function.identity()
                            )));
        }

        return memberMap;
    }


    public String getMeetupPath() {
        return meetupPath;
    }

    public Set<String> getHosts(String eventId) {
        Map<String, Object> params = new HashMap<String, Object>();

        params.put("meetup_url_path", getMeetupPath());
        params.put("event_id", eventId);
        params.put("key", getKey());

        log.info("getHosts() querying URL:" + MEMBERS_URL);
        ResponseEntity<Members.Member[]> responseEntity = restTemplate.getForEntity(HOSTS_URL, Members.Member[].class, params);

        return Stream.of(responseEntity.getBody()).map(Members.Member::getId).collect(Collectors.toSet());
    }
}
