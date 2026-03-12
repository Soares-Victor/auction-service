package com.auctionservice.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceBidRequest(
        @NotBlank String userId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
