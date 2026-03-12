package com.auctionservice.websocket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuctionClosedMessage(
        String type,
        UUID auctionId,
        String winnerId,
        BigDecimal winningBid,
        Instant closedAt
) {
}
