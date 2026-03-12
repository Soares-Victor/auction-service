package com.auctionservice.model;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank String userId,
        @NotBlank String content
) {
}
