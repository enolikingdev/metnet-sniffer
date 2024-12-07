package com.enoliking.metnetsniffer.jsoup;

import com.enoliking.metnetsniffer.influx.Temperature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DocumentProcessorTest {

    public static final String LOCATION = "Ráckeresztúr";
    public static final String TEST_RESOURCE = "/512-2024-11-30.html";

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
    public void getTemperatureTest() {
        List<Temperature> rows = processor.getTemperature(doc);
        assertEquals(145, rows.size());
        for (Temperature row : rows) {
            assertEquals(LOCATION, row.getLocation());
            assertNotNull(row.getValue());
            assertNotNull(row.getTime());
        }
    }
}
