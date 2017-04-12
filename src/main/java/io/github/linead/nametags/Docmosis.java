package io.github.linead.nametags;

import io.github.linead.nametags.domain.Attendee;
import io.github.linead.nametags.domain.DocmosisRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class Docmosis {

    @Value("${docmosis-api.renderurl}")
    private String host;

    @Value("${docmosis-api.key}")
    private String key;

    @Autowired
    private RestTemplate restTemplate;

    public byte[] render(Map<String, List<Attendee>> attendeeList) {
        DocmosisRequest req = newDocmosisRequest(attendeeList);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(host, req, byte[].class);
        return response.getBody();
    }

    public String getKey() { return key; }

    private DocmosisRequest newDocmosisRequest(Map<String, List<Attendee>> attendeeList) {
        DocmosisRequest req =new DocmosisRequest();
        req.setAccessKey(getKey());
        req.setTemplateName("template5.docx");
        req.setOutputName("all_labels.pdf");
        req.setData(attendeeList);
        return req;
    }

}
