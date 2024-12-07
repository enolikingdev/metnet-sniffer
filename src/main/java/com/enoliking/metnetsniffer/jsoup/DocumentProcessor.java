package com.enoliking.metnetsniffer.jsoup;

import com.enoliking.metnetsniffer.influx.Temperature;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentProcessor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String CELSIUS_SEPARATOR = " ";
    public static final String PERCENTAGE_SEPARATOR = "%";
    public static final String TIMEZONE = "Europe/Paris";

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
        String temperatureWithCelsius = elements.get(1).text();
        String dewWithCelsius = elements.get(2).text();
        String humidityWithPercentage = elements.get(3).text();
        return Temperature.builder()
                .time(toInstant(dateAsString))
                .temperature(stringToDouble(temperatureWithCelsius))
                .dew(stringToDouble(dewWithCelsius))
                .humidity(withoutPercentage(humidityWithPercentage))
                .location(location)
                .build();
    }

    private Instant toInstant(String dateAsString) {
        LocalDateTime dateTime = LocalDateTime.parse(dateAsString, FORMATTER);
        return ZonedDateTime.of(dateTime, ZoneId.of(TIMEZONE)).toInstant();
    }

    @NotNull
    private Double stringToDouble(String withCelsius) {
        return Double.valueOf(withoutCelsius(withCelsius));
    }

    @NotNull
    private String withoutCelsius(String withCelsius) {
        return cutBefore(withCelsius, CELSIUS_SEPARATOR);
    }

    @NotNull
    private int withoutPercentage(String withPercentage) {
        return Integer.valueOf(cutBefore(withPercentage, PERCENTAGE_SEPARATOR));
    }

    private String cutBefore(String input, String cutHere) {
        return input.substring(0, input.indexOf(cutHere));
    }

}
