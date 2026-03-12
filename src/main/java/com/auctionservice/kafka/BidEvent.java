package com.auctionservice.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidEvent(
        UUID eventId,
        UUID auctionId,
        String userId, // user posting the BID
        BigDecimal amount,
        Instant publishedAt
) {
}
