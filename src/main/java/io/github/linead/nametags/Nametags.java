package io.github.linead.nametags;

import io.github.linead.nametags.domain.Attendee;
import io.github.linead.nametags.domain.Event;
import io.github.linead.nametags.domain.Members;
import io.github.linead.nametags.domain.Rsvp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@SpringBootApplication
@EnableCaching
@RestController
public class Nametags extends SpringBootServletInitializer implements CommandLineRunner {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MeetupData meetup;

    @Autowired
    Docmosis docmosis;

    @Value("${sendPictureAsBase64}")
    private boolean sendPictureAsBase64;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Nametags.class, args);
    }

    @RequestMapping("/events")
    public List<Event> getEvents() { return Arrays.asList(meetup.getNextMeetups()); }

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

                    String pictureURLStr = member.getPhoto().getThumb_link();
                    log.info("getAttendeeList() photo URL:" + pictureURLStr);
                    if(sendPictureAsBase64) {
                        // Retrieve image data and encode it
                        att.setPictureData(encodeImageData(pictureURLStr));
                    } else {
                        // Pass image URL on to Docmosis
                        att.setPictureUrl(pictureURLStr);
                    }
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
    public void run(String... strings) throws Exception { }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("members");
    }

    public Set<String> getHosts(String eventId) {
        return meetup.getHosts(eventId);
    }

    private String encodeImageData(String pictureURLStr) {
        String imageData = "";

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = new URL(pictureURLStr).openStream();
            int reads = is.read();
            while(reads != -1) {
                baos.write(reads);
                reads = is.read();
            }
            imageData = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch(MalformedURLException e) {
            log.error("getAttendeeList() Bad photo URL:" + e.getMessage());
        } catch(IOException e) {
            log.error("getAttendeeList() Error getting image data:" + e.getMessage());
        }

        return imageData;
    }
}