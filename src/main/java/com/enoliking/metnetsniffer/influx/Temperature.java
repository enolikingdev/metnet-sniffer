package com.enoliking.metnetsniffer.influx;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Measurement(name = "temperature")
@Builder
@Getter
public class Temperature {

    @Column(tag = true)
    String location;

    @Column
    Double value;

    @Column(timestamp = true)
    Instant time;
}
