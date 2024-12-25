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
import java.util.Objects;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

@Component
public class DocumentProcessor {

    public static final String LAT_PREFIX = "Lat: ";
    public static final String LON_PREFIX = "Lon: ";
    public static final String LAT_LON_SEPARATOR = " , ";

    record Position(double lat, double lon) {}

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String SPACE = " ";
    public static final String PERCENTAGE_SEPARATOR = "%";
    public static final String TIMEZONE = "Europe/Paris";

    public String getLocation(Document doc) {
        return Objects.requireNonNull(doc.selectFirst(".m3")).text();
    }

    public int getId(Document doc) {
        return parseInt(Objects.requireNonNull(doc.selectFirst("[name=\"ostid\"]")).val());
    }

    public Position getPosition(Document doc) {
        String positionString = doc.select(".p1").get(1).text();
        String withoutPrefix = cutAfter(positionString, LAT_PREFIX);
        String lat = cutBefore(withoutPrefix, LAT_LON_SEPARATOR);
        String lon = cutAfter(withoutPrefix, LON_PREFIX);
        return new Position(parseDouble(lat), parseDouble(lon));
    }

    public List<Temperature> getTemperature(Document doc) {
        int id = getId(doc);
        String location = getLocation(doc);
        Position position = getPosition(doc);
        Elements rows = doc.select("tr");
        return rows.stream()
                .skip(1)
                .map(Element::children)
                .map(rowCells -> mapToTemperature(rowCells, id, location, position))
                .toList();
    }

    private Temperature mapToTemperature(Elements elements, int id, String location, Position position) {
        String dateAsString = elements.get(0).text();
        String temperatureWithCelsius = elements.get(1).text();
        String dewWithCelsius = elements.get(2).text();
        String humidityWithPercentage = elements.get(3).text();
        String rainFallWithMillimeter = elements.get(7).text();
        return Temperature.builder()
                .id(id)
                .time(toInstant(dateAsString))
                .temperature(stringToDouble(temperatureWithCelsius))
                .dew(stringToDouble(dewWithCelsius))
                .humidity(withoutPercentage(humidityWithPercentage))
                .rainfall(withoutMillimeter(rainFallWithMillimeter))
                .location(location)
                .positionLat(position.lat)
                .positionLon(position.lon)
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
        return cutBefore(withCelsius, SPACE);
    }

    private int withoutPercentage(String withPercentage) {
        return parseInt(cutBefore(withPercentage, PERCENTAGE_SEPARATOR));
    }

    private Double withoutMillimeter(String withMillimeter) {
        return parseDouble(cutBefore(withMillimeter, SPACE));
    }

    private String cutBefore(String input, String cutHere) {
        return input.substring(0, input.indexOf(cutHere));
    }

    private String cutAfter(String input, String cutHere) {
        return input.substring(input.indexOf(cutHere) + cutHere.length());
    }

}
