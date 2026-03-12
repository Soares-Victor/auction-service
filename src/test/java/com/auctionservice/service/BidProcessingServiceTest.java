package com.auctionservice.service;

import com.auctionservice.TestUtils;
import com.auctionservice.entity.Auction;
import com.auctionservice.entity.Bid;
import com.auctionservice.kafka.BidEvent;
import com.auctionservice.repository.AuctionRepository;
import com.auctionservice.repository.BidRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidProcessingServiceTest {

    @Mock AuctionRepository auctionRepository;
    @Mock BidRepository     bidRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks BidProcessingService service;

    @Test
    void returnsFalse_whenAuctionNotFound() {
        UUID auctionId = UUID.randomUUID();
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        boolean result = service.process(event(auctionId, "alice", "150.00"));

        assertThat(result).isFalse();
        verify(bidRepository, never()).save(any());
    }

    @Test
    void returnsFalse_whenAuctionIsScheduled() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.scheduledAuction(auctionId);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        boolean result = service.process(event(auctionId, "alice", "150.00"));

        assertThat(result).isFalse();
        verify(bidRepository, never()).save(any());
    }

    @Test
    void returnsFalse_whenAuctionIsClosed() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.closedAuction(auctionId);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        boolean result = service.process(event(auctionId, "alice", "150.00"));

        assertThat(result).isFalse();
        verify(bidRepository, never()).save(any());
    }

    @Test
    void returnsFalse_whenUserIsAlreadyCurrentLeader() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(auctionId);
        Bid leader = TestUtils.bid(UUID.randomUUID(), auctionId, "alice", new BigDecimal("200.00"));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(Optional.of(leader));

        boolean result = service.process(event(auctionId, "alice", "250.00"));

        assertThat(result).isFalse();
        verify(bidRepository, never()).save(any());
    }

    @Test
    void returnsFalse_whenAmountBelowStartingPrice_andNoBidsYet() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(auctionId); // startingPrice = 100.00

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(Optional.empty());

        boolean result = service.process(event(auctionId, "alice", "50.00"));

        assertThat(result).isFalse();
        verify(bidRepository, never()).save(any());
    }

    @Test
    void returnsFalse_whenAmountBelowMinimumIncrement() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(auctionId); // minBidIncrement = 10.00
        Bid leader = TestUtils.bid(UUID.randomUUID(), auctionId, "bob", new BigDecimal("200.00"));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(Optional.of(leader));

        // min valid = 200.00 + 10.00 = 210.00; submitting 205.00 is insufficient
        boolean result = service.process(event(auctionId, "alice", "205.00"));

        assertThat(result).isFalse();
        verify(bidRepository, never()).save(any());
    }

    @Test
    void returnsTrue_andPersistsBid_whenValidFirstBid() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(auctionId); // startingPrice = 100.00
        Bid savedBid = TestUtils.bid(UUID.randomUUID(), auctionId, "alice", new BigDecimal("100.00"));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.save(any())).thenReturn(savedBid);

        boolean result = service.process(event(auctionId, "alice", "100.00"));

        assertThat(result).isTrue();
        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("alice");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void returnsTrue_andPersistsBid_whenOutbiddingCurrentLeader() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(auctionId); // minBidIncrement = 10.00
        Bid leader = TestUtils.bid(UUID.randomUUID(), auctionId, "bob", new BigDecimal("200.00"));
        Bid savedBid = TestUtils.bid(UUID.randomUUID(), auctionId, "alice", new BigDecimal("210.00"));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(Optional.of(leader));
        when(bidRepository.save(any())).thenReturn(savedBid);

        boolean result = service.process(event(auctionId, "alice", "210.00"));

        assertThat(result).isTrue();
        verify(bidRepository).save(any());
    }

    @Test
    void broadcastsWebSocketMessage_onSuccessfulBid() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(auctionId);
        Bid savedBid = TestUtils.bid(UUID.randomUUID(), auctionId, "alice", new BigDecimal("100.00"));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(Optional.empty());
        when(bidRepository.save(any())).thenReturn(savedBid);

        service.process(event(auctionId, "alice", "100.00"));

        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), (Object) any());
    }

    private BidEvent event(UUID auctionId, String userId, String amount) {
        return new BidEvent(UUID.randomUUID(), auctionId, userId, new BigDecimal(amount), Instant.now());
    }
}
