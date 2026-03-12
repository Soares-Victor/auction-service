package com.auctionservice.model;

import com.auctionservice.entity.AuctionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuctionResponse(
        UUID id,
        String sellerId,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        BigDecimal startingPrice,
        BigDecimal minBidIncrement,
        AuctionStatus status,
        BigDecimal currentHighestBid,
        String currentLeaderId,
        Instant createdAt
) {
}
