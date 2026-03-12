package com.auctionservice.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionStatusTest {

    @Test
    void returnsScheduled_whenNowIsBeforeStartTime() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end   = Instant.now().plusSeconds(7200);

        assertThat(AuctionStatus.compute(start, end)).isEqualTo(AuctionStatus.SCHEDULED);
    }

    @Test
    void returnsOpen_whenNowIsBetweenStartAndEnd() {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end   = Instant.now().plusSeconds(3600);

        assertThat(AuctionStatus.compute(start, end)).isEqualTo(AuctionStatus.OPEN);
    }

    @Test
    void returnsClosed_whenNowIsAfterEndTime() {
        Instant start = Instant.now().minusSeconds(7200);
        Instant end   = Instant.now().minusSeconds(3600);

        assertThat(AuctionStatus.compute(start, end)).isEqualTo(AuctionStatus.CLOSED);
    }
}
