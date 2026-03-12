package com.auctionservice.websocket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidPlacedMessage(
        String type,
        UUID auctionId,
        UUID bidId,
        String userId,
        BigDecimal amount,
        Instant timestamp
) {
}
