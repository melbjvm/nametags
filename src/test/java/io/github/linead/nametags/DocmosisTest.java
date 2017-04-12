package io.github.linead.nametags;

import io.github.linead.nametags.domain.DocmosisRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static org.assertj.core.util.Maps.newHashMap;
import static org.mockito.Mockito.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DocmosisTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private Docmosis docmosis;

    @Test
    public void renderSendsGETRequestToDocmosisRenderUrl() {
        byte[] sampleResponse = "test body".getBytes();

        when(restTemplate.postForEntity(anyString(), any(DocmosisRequest.class), eq(byte[].class)))
            .thenReturn(ResponseEntity.ok(sampleResponse));

        // when
        byte[] pdfBytes = docmosis.render(newHashMap("foo", emptyList()));

        assertThat(pdfBytes).containsSequence(sampleResponse);
    }

}