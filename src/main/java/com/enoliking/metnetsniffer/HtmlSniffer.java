package com.enoliking.metnetsniffer;

import com.enoliking.metnetsniffer.influx.Temperature;
import com.enoliking.metnetsniffer.jsoup.DocumentProcessor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HtmlSniffer {

    private final SSLHelper sslHelper;
    private final DocumentProcessor processor;

    @Value("${user.agent}")
    private String userAgent;
    private String url = "http://www.metnet.hu/online-allomasok?sub=showosdata&ostid=512&date=2024-11-30";

    @PostConstruct
    public List<Temperature> sniff() throws IOException {
        Document doc = sslHelper.getConnection(url).userAgent(userAgent).get();
        return processor.getTemperature(doc);
    }

}
