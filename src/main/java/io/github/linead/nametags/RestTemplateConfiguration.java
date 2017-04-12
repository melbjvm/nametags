package io.github.linead.nametags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class RestTemplateConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateConfiguration.class);

    @Bean
    public RestTemplate loggingRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new LoggingResponseErrorHandler());

        return restTemplate;
    }

    class LoggingResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            try {
                super.handleError(response);
            } catch (RestClientResponseException exception) {
                LOG.error("Error received calling API - ({} {}): Headers {}, Body: {}",
                        exception.getRawStatusCode(),
                        exception.getStatusText(),
                        Arrays.deepToString(response.getHeaders().entrySet().toArray()),
                        exception.getResponseBodyAsString(),
                        exception);
                throw exception;
            }
        }
    }

}
