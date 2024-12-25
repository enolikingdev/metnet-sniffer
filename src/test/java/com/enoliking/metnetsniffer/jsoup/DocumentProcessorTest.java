package com.enoliking.metnetsniffer.jsoup;

import com.enoliking.metnetsniffer.influx.Temperature;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.enoliking.metnetsniffer.HtmlSniffer.FORMATTER;
import static com.enoliking.metnetsniffer.jsoup.DocumentProcessor.TIMEZONE;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class DocumentProcessorTest {

    public static final String DAY = "2024-11-30";
    public static final String LOCATION = "Ráckeresztúr";
    public static final double POSITION_LAT = 47.2757;
    public static final double POSITION_LON = 18.8307;
    public static final int OSTID = 512;
    public static final String TEST_RESOURCE = "/" + OSTID + "-" + DAY + ".html";

    private LocalDate day = LocalDate.parse(DAY, FORMATTER);
    private Instant start = day.atStartOfDay(ZoneId.of(TIMEZONE)).toInstant().minusSeconds(1);
    private Instant end = day.plusDays(1).atStartOfDay(ZoneId.of(TIMEZONE)).toInstant().plusSeconds(1);

    private DocumentProcessor processor = new DocumentProcessor();
    private Document doc;

    @BeforeEach
    public void before() throws URISyntaxException, IOException {
        String documentContent = Files.readString(Paths.get(getClass().getResource(TEST_RESOURCE).toURI()));
        doc = Jsoup.parse(documentContent);
    }

    @Test
    public void getLocationTest() {
        String location = processor.getLocation(doc);
        assertEquals(LOCATION, location);
    }

    @Test
    public void getIdTest() {
        int id = processor.getId(doc);
        assertEquals(OSTID, id);
    }

    @Test
    public void getPositionTest() {
        DocumentProcessor.Position position = processor.getPosition(doc);
        assertEquals(POSITION_LAT, position.lat());
        assertEquals(POSITION_LON, position.lon());
    }

    @Test
    public void getTemperatureTest() {
        List<Temperature> rows = processor.getTemperature(doc);
        assertEquals(145, rows.size());
        for (Temperature row : rows) {
            assertEquals(OSTID, row.getId());
            assertEquals(LOCATION, row.getLocation());
            assertEquals(POSITION_LAT, row.getPositionLat());
            assertEquals(POSITION_LON, row.getPositionLon());
            assertNotNull(row.getTemperature());
            assertNotNull(row.getDew());
            assertNotNull(row.getHumidity());
            assertNotNull(row.getTime());
            assertNotNull(row.getTemperature());
            assertTrue(validTime(row.getTime()));
        }
    }

    private boolean validTime(Instant time) {
        if (time.isAfter(start) && time.isBefore(end)) {
            return true;
        }
        log.info(time.toString());
        return false;
    }
}
