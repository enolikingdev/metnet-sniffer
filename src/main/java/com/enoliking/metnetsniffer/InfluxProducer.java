package com.enoliking.metnetsniffer;

import com.enoliking.metnetsniffer.influx.PointConverter;
import com.enoliking.metnetsniffer.influx.Temperature;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import static com.enoliking.metnetsniffer.jsoup.DocumentProcessor.TIMEZONE;

@Profile("prod")
@Component
@RequiredArgsConstructor
public class InfluxProducer {

    @Value("${influx.host:localhost}")
    private String host;
    @Value("${influx.token:sometoken}")
    private String token;
    @Value("${influx.org:myorg}")
    private String org;
    @Value("${influx.bucket:mybucket}")
    private String bucket;
    @Value("${metnet.ostid}")
    private int ostid;
    @Value("${ttl:10}")
    private int ttlSeconds;

    private final HtmlSniffer htmlSniffer;
    private final PointConverter pointConverter;

    @PostConstruct
    public void doit() throws IOException {
        LocalDate day = LocalDate.now();
        List<Temperature> temperatureList = htmlSniffer.sniff(day);

        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://" + host + ":8086", token.toCharArray(), org, bucket);
        List<Instant> whatWeHave = readData(influxDBClient, day);

        List<Temperature> filteredTemperatureList = getMissing(temperatureList, whatWeHave);
        List<Point> points = filteredTemperatureList.stream()
                .map(pointConverter::convert)
                .flatMap(Collection::stream)
                .toList();
        writeData(influxDBClient, points);
        influxDBClient.close();
        try {
            Thread.sleep(ttlSeconds * 1000); // Simulate some work
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<Temperature> getMissing(List<Temperature> temperatureList, List<Instant> whatWeHave) {
        return temperatureList.stream()
                .filter(temperature -> !whatWeHave.contains(temperature.getTime()))
                .toList();
    } 
    
    private List<Instant> readData(InfluxDBClient influxDBClient, LocalDate localDate) {
        Instant startOfDay = localDate.atStartOfDay(ZoneId.of(TIMEZONE)).toInstant();
        Instant endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.of(TIMEZONE)).toInstant();
        String flux = "from(bucket:\"" + bucket + "\") |> range(start: " + startOfDay + ", stop: " + endOfDay + ")" +
                "|> filter(fn: (r) => r[\"id\"] == \"" + ostid +"\")";

        QueryApi queryApi = influxDBClient.getQueryApi();

        List<FluxTable> tables = queryApi.query(flux);
        List<Instant> list = tables.stream()
                .map(FluxTable::getRecords)
                .flatMap(Collection::stream)
                .map(FluxRecord::getTime)
                .distinct()
                .toList();
        return list;
    }

    private void writeData(InfluxDBClient influxDBClient, List<Point> points) {
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        writeApi.writePoints(points);
    }

}
