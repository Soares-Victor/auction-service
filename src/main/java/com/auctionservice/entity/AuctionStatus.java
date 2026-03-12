package com.auctionservice.entity;

import java.time.Instant;

public enum AuctionStatus {
    SCHEDULED, OPEN, CLOSED;

    public static AuctionStatus compute(Instant startTime, Instant endTime) {
        Instant now = Instant.now();
        if (now.isBefore(startTime)) return SCHEDULED;
        if (now.isAfter(endTime))    return CLOSED;
        return OPEN;
    }
}
