package com.airtribe.smartparking.pricing;

import com.airtribe.smartparking.model.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;

// Free grace period, then every started hour is charged at the vehicle's hourly rate
public class HourlyPricingStrategy implements PricingStrategy {

    private static final long SECONDS_PER_HOUR = 3600;

    private final RateCard rateCard;
    private final Duration gracePeriod;

    public HourlyPricingStrategy(RateCard rateCard, Duration gracePeriod) {
        this.rateCard = rateCard;
        this.gracePeriod = gracePeriod;
    }

    @Override
    public BigDecimal calculateFee(VehicleType type, Duration stay) {
        if (stay.isNegative()) {
            throw new IllegalArgumentException("Stay duration cannot be negative");
        }
        if (stay.compareTo(gracePeriod) <= 0) {
            return BigDecimal.ZERO; // inside the grace period: free
        }
        long hours = (stay.getSeconds() + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR; // round UP to full hours
        return rateCard.getHourlyRate(type).multiply(BigDecimal.valueOf(hours));
    }
}