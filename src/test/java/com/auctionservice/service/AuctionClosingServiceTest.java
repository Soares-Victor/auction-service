package com.auctionservice.service;

import com.auctionservice.TestUtils;
import com.auctionservice.entity.Auction;
import com.auctionservice.entity.Bid;
import com.auctionservice.repository.AuctionRepository;
import com.auctionservice.repository.BidRepository;
import com.auctionservice.websocket.AuctionClosedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class AuctionClosingServiceTest {

    @Mock AuctionRepository    auctionRepository;
    @Mock BidRepository        bidRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks AuctionClosingService service;

    @Test
    void broadcastsClosedMessage_withWinner_whenBidsExist() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.closedAuction(auctionId);
        Bid winner = TestUtils.bid(UUID.randomUUID(), auctionId, "alice", new BigDecimal("500.00"));

        when(auctionRepository.findByEndTimeBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAsc(auctionId))
                .thenReturn(Optional.of(winner));

        service.broadcastJustClosedAuctions();

        ArgumentCaptor<AuctionClosedMessage> captor = ArgumentCaptor.forClass(AuctionClosedMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), captor.capture());

        AuctionClosedMessage message = captor.getValue();
        assertThat(message.type()).isEqualTo("AUCTION_CLOSED");
        assertThat(message.auctionId()).isEqualTo(auctionId);
        assertThat(message.winnerId()).isEqualTo("alice");
        assertThat(message.winningBid()).isEqualByComparingTo("500.00");
    }

    @Test
    void broadcastsClosedMessage_withNullWinner_whenNoBidsExist() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.closedAuction(auctionId);

        when(auctionRepository.findByEndTimeBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAsc(auctionId))
                .thenReturn(Optional.empty());

        service.broadcastJustClosedAuctions();

        ArgumentCaptor<AuctionClosedMessage> captor = ArgumentCaptor.forClass(AuctionClosedMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), captor.capture());

        assertThat(captor.getValue().winnerId()).isNull();
        assertThat(captor.getValue().winningBid()).isNull();
    }

    @Test
    void doesNotBroadcast_whenNoAuctionsJustClosed() {
        when(auctionRepository.findByEndTimeBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        service.broadcastJustClosedAuctions();

        verify(messagingTemplate, never()).convertAndSend(any(String.class), (Object) any());
    }
}
