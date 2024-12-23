package com.enoliking.metnetsniffer.influx;

@FunctionalInterface
public interface NumericValueConverter {

    Number getValue(Temperature temperature);

}
