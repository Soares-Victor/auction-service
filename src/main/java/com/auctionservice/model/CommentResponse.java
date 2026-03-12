package com.auctionservice.model;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID auctionId,
        String userId,
        String content,
        Instant createdAt
) {
}
