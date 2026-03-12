package com.auctionservice.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateAuctionRequest(
        @NotBlank String sellerId,
        @NotBlank String title,
        String description,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull @DecimalMin("0.01") BigDecimal startingPrice,
        @NotNull @DecimalMin("0.01") BigDecimal minBidIncrement
) {
}
