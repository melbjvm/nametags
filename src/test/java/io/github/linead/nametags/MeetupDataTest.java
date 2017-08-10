package io.github.linead.nametags;

import io.github.linead.nametags.domain.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MeetupDataTest {

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("https://api.meetup.com/Melbourne-Java-JVM-Users-Group/events?scroll=recent_past&photo-host=public&page=20&key=MyAPIKey"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withSuccess(new ClassPathResource("meetups.json"), MediaType.APPLICATION_JSON));
    }

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    MeetupData meetupData;

    @Test
    public void getNextMeetupsWillReturnAFutureMeetup() throws Exception {
        Event[] events = meetupData.getNextMeetups();

        assertThat(events, hasItemInArray(hasProperty("time", equalTo(1462350600000l))));
    }

    @Test
    public void retrievedEventHaveValidName() {
        Event[] events = meetupData.getNextMeetups();

        assertThat(asList(events), everyItem(hasProperty("name", not(isEmptyString()))));
    }

    @Test
    public void retrievedEventHaveValidDuration() {
        Event[] events = meetupData.getNextMeetups();

        assertThat(asList(events), everyItem(hasProperty("duration", greaterThan(0L))));
    }
}