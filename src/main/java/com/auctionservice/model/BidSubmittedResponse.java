package com.auctionservice.model;

import java.util.UUID;

public record BidSubmittedResponse(
        String message,
        UUID eventId
) {
}
