package com.auctionservice;

import com.auctionservice.entity.Auction;
import com.auctionservice.entity.BaseEntity;
import com.auctionservice.entity.Bid;
import com.auctionservice.entity.Comment;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TestUtils {

    private TestUtils() {}

    public static void setId(BaseEntity entity, UUID id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Auction openAuction() {
        return openAuction(UUID.randomUUID());
    }

    public static Auction openAuction(UUID id) {
        Auction a = new Auction();
        a.setSellerId("seller1");
        a.setTitle("Test Auction");
        a.setDescription("A test auction item");
        a.setStartTime(Instant.now().minusSeconds(3600));
        a.setEndTime(Instant.now().plusSeconds(3600));
        a.setStartingPrice(new BigDecimal("100.00"));
        a.setMinBidIncrement(new BigDecimal("10.00"));
        setId(a, id);
        return a;
    }

    public static Auction scheduledAuction(UUID id) {
        Auction a = new Auction();
        a.setSellerId("seller1");
        a.setTitle("Upcoming Auction");
        a.setDescription("Starts in the future");
        a.setStartTime(Instant.now().plusSeconds(3600));
        a.setEndTime(Instant.now().plusSeconds(7200));
        a.setStartingPrice(new BigDecimal("100.00"));
        a.setMinBidIncrement(new BigDecimal("10.00"));
        setId(a, id);
        return a;
    }

    public static Auction closedAuction(UUID id) {
        Auction a = new Auction();
        a.setSellerId("seller1");
        a.setTitle("Past Auction");
        a.setDescription("Already ended");
        a.setStartTime(Instant.now().minusSeconds(7200));
        a.setEndTime(Instant.now().minusSeconds(3600));
        a.setStartingPrice(new BigDecimal("100.00"));
        a.setMinBidIncrement(new BigDecimal("10.00"));
        setId(a, id);
        return a;
    }

    public static Bid bid(UUID id, UUID auctionId, String userId, BigDecimal amount) {
        Bid b = new Bid();
        b.setAuctionId(auctionId);
        b.setUserId(userId);
        b.setAmount(amount);
        setId(b, id);
        return b;
    }

    public static Comment comment(UUID id, UUID auctionId, String userId, String content) {
        Comment c = new Comment();
        c.setAuctionId(auctionId);
        c.setUserId(userId);
        c.setContent(content);
        setId(c, id);
        return c;
    }
}
