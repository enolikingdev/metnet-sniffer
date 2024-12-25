package com.enoliking.metnetsniffer.influx;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PointConverter {

    public List<Point> convert(Temperature input) {
        Point temperaturePoint = toPoint(input,"°C","temperature", Temperature::getTemperature);
        Point dewPoint = toPoint(input,"°C", "dew", Temperature::getDew);
        Point humidityPoint = toPoint(input,"%", "humidity", Temperature::getHumidity);
        Point rainFallPoint = toPoint(input,"mm", "rainFall", Temperature::getRainfall);
        return List.of(temperaturePoint, dewPoint, humidityPoint, rainFallPoint);
    }

    private Point toPoint(Temperature input, String measurement, String label, NumericValueConverter converter) {
        return Point.measurement(measurement)
                .addTag("id", String.valueOf(input.getId()))
                .addTag("location", input.getLocation())
                .addTag("positionLat", String.valueOf(input.getPositionLat()))
                .addTag("positionLon", String.valueOf(input.getPositionLon()))
                .addTag("domain", "metnet")
                .addTag("label", label)
                .addField("value", converter.getValue(input))
                .time(input.getTime(), WritePrecision.MS);
    }

}
