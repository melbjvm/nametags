package io.github.linead.nametags;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.shouldHaveThrown;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;


@RunWith(SpringRunner.class)
@SpringBootTest
public class RestTemplateConfigurationTest {

    public static final String FAILURE_JSON = "{ \"message\": \"A client error\", \"code\": 400 }";
    public static final String SERVER_FAILURE_JSON = "{ \"message\": \"A server error\", \"code\": 500 }";

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer server;

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Test
    public void hasByteArrayMessageConverterToOutputPDFs() {
        assertThat(restTemplate.getMessageConverters()).hasAtLeastOneElementOfType(ByteArrayHttpMessageConverter.class);
    }

    @Test
    public void injectedRestTemplateHasLoggingErrorHandlerConfigured() {
        assertThat(restTemplate.getErrorHandler()).isInstanceOf(RestTemplateConfiguration.LoggingResponseErrorHandler.class);
    }

    @Before
    public void bindServerToRestTemplate() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void a4XXresponseCodeBodyIsLogged() {
        configureMockRestServiceToFailOn400LevelError();

        assertThatThrownBy(() -> restTemplate.getForObject("/fail/4xx", String.class))
                .isInstanceOf(HttpClientErrorException.class);

        String capturedOutput = capture.toString();
        assertThat(capturedOutput)
                .contains("Error received calling API - ")
                .contains("400 Bad Request")
                .contains("Headers [Content-Type=[application/json]]")
                .contains("Body: " + FAILURE_JSON)
                .contains(HttpClientErrorException.class.getCanonicalName());

        server.verify();

    }

    private void configureMockRestServiceToFailOn400LevelError() {
        server.expect(requestTo("/fail/4xx"))
                .andRespond(
                        withBadRequest()
                                .body(FAILURE_JSON)
                                .contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void a5XXresponseCodeBodyIsLogged() {
        configureMockRestServiceToFailWithServerError();

        assertThatThrownBy(() -> restTemplate.getForObject("/fail/5xx", String.class))
                .isInstanceOf(HttpServerErrorException.class);

        String capturedOutput = capture.toString();
        assertThat(capturedOutput)
                .contains("Error received calling API - ")
                .contains("(500 Internal Server Error)")
                .contains("Headers [Content-Type=[application/json]]")
                .contains("Body: " + SERVER_FAILURE_JSON)
                .contains(HttpServerErrorException.class.getCanonicalName());


        server.verify();
    }

    private void configureMockRestServiceToFailWithServerError() {
        server.expect(requestTo("/fail/5xx"))
                .andRespond(
                        withServerError()
                                .body(SERVER_FAILURE_JSON)
                                .contentType(MediaType.APPLICATION_JSON));
    }
}