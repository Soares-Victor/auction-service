package com.auctionservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidResponse(
        UUID id,
        UUID auctionId,
        String userId,
        BigDecimal amount,
        Instant createdAt
) {
}
