package com.airtribe.smartparking.pricing;

import com.airtribe.smartparking.model.VehicleType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

// Hourly price per vehicle type, kept as data so rates can change without code changes
public class RateCard {

    private final Map<VehicleType, BigDecimal> hourlyRates;

    public RateCard(Map<VehicleType, BigDecimal> hourlyRates) {
        this.hourlyRates = new EnumMap<VehicleType, BigDecimal>(hourlyRates); // defensive copy
        for (VehicleType type : VehicleType.values()) {
            if (!this.hourlyRates.containsKey(type)) {
                throw new IllegalArgumentException("Missing hourly rate for " + type); // fail fast at start-up
            }
        }
    }

    public static RateCard defaultRates() {
        Map<VehicleType, BigDecimal> rates = new EnumMap<VehicleType, BigDecimal>(VehicleType.class);
        rates.put(VehicleType.MOTORCYCLE, new BigDecimal("10"));
        rates.put(VehicleType.CAR, new BigDecimal("20"));
        rates.put(VehicleType.BUS, new BigDecimal("50"));
        return new RateCard(rates);
    }

    public BigDecimal getHourlyRate(VehicleType type) {
        return hourlyRates.get(type);
    }
}