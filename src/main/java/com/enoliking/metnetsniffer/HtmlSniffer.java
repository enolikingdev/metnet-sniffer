package com.enoliking.metnetsniffer;

import com.enoliking.metnetsniffer.influx.Temperature;
import com.enoliking.metnetsniffer.jsoup.DocumentProcessor;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;

import static com.enoliking.metnetsniffer.jsoup.DocumentProcessor.TIMEZONE;

@Component
@RequiredArgsConstructor
public class HtmlSniffer {

    public static final String URL = "http://www.metnet.hu/online-allomasok";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SSLHelper sslHelper;
    private final DocumentProcessor processor;

    @Value("${user.agent}")
    private String userAgent;
    @Value("${metnet.ostid}")
    private String ostid;

    public List<Temperature> sniff(LocalDate date) throws IOException {
        Document doc = sslHelper.getConnection(buildUrl(date)).userAgent(userAgent).get();
        List<Temperature> temperatureList = processor.getTemperature(doc);

        ZoneId zoneId = ZoneId.of(TIMEZONE);
        Predicate<Temperature> isAtGivenDay = temperature ->
                LocalDate.ofInstant(temperature.getTime(), zoneId).equals(date);
        List<Temperature> list = temperatureList.stream()
                .filter(isAtGivenDay)
                .toList();
        return list;
    }

    private String buildUrl(LocalDate date) {
        return URL + "?sub=showosdata&ostid=" + ostid + "&date=" + date.format(FORMATTER);
    }

}
