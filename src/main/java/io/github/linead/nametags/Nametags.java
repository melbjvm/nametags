package io.github.linead.nametags;

import io.github.linead.nametags.domain.Attendee;
import io.github.linead.nametags.domain.Event;
import io.github.linead.nametags.domain.Members;
import io.github.linead.nametags.domain.Rsvp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@SpringBootApplication
@EnableCaching
@RestController
public class Nametags extends SpringBootServletInitializer implements CommandLineRunner {

    @Autowired
    MeetupData meetup;

    @Autowired
    Docmosis docmosis;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Nametags.class, args);
    }

    @RequestMapping("/events")
    public List<Event> getEvents() {
        return Arrays.asList(meetup.getNextMeetups());
    }

    @RequestMapping("/attendees")
    public Map<String, List<Attendee>> getAttendeeList(@RequestParam(value = "eventId") String eventId) {

        List<Attendee> attendees = new ArrayList<>();

        Event event = Arrays.asList(meetup.getNextMeetups())
                .stream().filter(e -> eventId.equals(e.getId())).findFirst().get();

        //fetch member info
        Map<String, Members.Member> members = meetup.getMembers(event.getGroup().getId());

        //fetch rsvps
        for(Rsvp rsvp : meetup.getRsvps(eventId).getResults()) {
            if(rsvp.getMember().getMember_id() == null) {
                continue;
            }

            if("yes".equals(rsvp.getStatus())) {
                continue;
            }


            //new attendee
            Attendee att = new Attendee();
            att.setId(rsvp.getMember().getMember_id());
            att.setName(rsvp.getMember().getName());
            //get member
            Members.Member member = members.get(rsvp.getMember().getMember_id());
            if(member != null) {
                att.setJoined(member.getJoined());
                if(member.getPhoto() != null) {
                    att.setPictureUrl(member.getPhoto().getThumb_link());
                } else {
                    att.setPictureUrl("http://photos3.meetupstatic.com/photos/event/7/3/a/2/global_414329602.jpeg");
                }
            }

            attendees.add(att);
        }

        attendees.sort(Comparator.comparing(Attendee::getName, String.CASE_INSENSITIVE_ORDER));
        Map<String, List<Attendee>> resultMap = new HashMap<>();
        resultMap.put("attendees", attendees);
        return resultMap;

    }

    @RequestMapping(value = "/nametags", produces = "application/x-pdf")
    public HttpEntity<byte[]> getNameTags(HttpServletResponse response, @RequestParam(value = "eventId") String eventId) {

        response.setHeader("Content-Disposition",
                "attachment; filename=" + eventId +"_"
                        + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
                        + "_nametags.pdf");

        byte[] pdf = docmosis.render(getAttendeeList(eventId));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new HttpEntity<byte[]>(pdf, headers);

    }

    @Override
    public void run(String... strings) throws Exception {
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("members");
    }

    public Set<String> getHosts(String eventId) {
        return meetup.getHosts(eventId);
    }
}