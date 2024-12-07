package com.enoliking.metnetsniffer.jsoup;

import com.enoliking.metnetsniffer.influx.Temperature;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentProcessor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String getLocation(Document doc) {
        Element element = doc.selectFirst(".m3");
        return element.text();
    }

    public List<Temperature> getTemperature(Document doc) {
        String location = getLocation(doc);
        Elements rows = doc.select("tr");
        return rows.stream()
                .skip(1)
                .map(row -> row.children())
                .map(rowCells -> mapToTemperature(rowCells, location))
                .collect(Collectors.toUnmodifiableList());
    }

    private Temperature mapToTemperature(Elements elements, String location) {
        String dateAsString = elements.get(0).text();
        String withCelsius = elements.get(1).text();
        return Temperature.builder()
                .time(toInstant(dateAsString))
                .value(stringToDouble(withCelsius))
                .location(location)
                .build();
    }

    private Instant toInstant(String dateAsString) {
        return LocalDateTime.parse(dateAsString, FORMATTER).toInstant(ZoneOffset.UTC);
    }

    @NotNull
    private static Double stringToDouble(String withCelsius) {
        return Double.valueOf(withoutCelsius(withCelsius));
    }

    @NotNull
    private static String withoutCelsius(String withCelsius) {
        return withCelsius.substring(0, withCelsius.indexOf(" "));
    }

}
