package com.auctionservice.service;

import com.auctionservice.TestUtils;
import com.auctionservice.entity.Auction;
import com.auctionservice.exception.AuctionNotOpenException;
import com.auctionservice.kafka.BidEvent;
import com.auctionservice.kafka.BidProducer;
import com.auctionservice.mapper.BidMapper;
import com.auctionservice.model.BidSubmittedResponse;
import com.auctionservice.model.PlaceBidRequest;
import com.auctionservice.repository.BidRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock AuctionService auctionService;
    @Mock BidRepository  bidRepository;
    @Mock BidProducer    bidProducer;
    @Mock BidMapper      bidMapper;

    @InjectMocks BidService bidService;

    @Test
    void placeBid_publishesEventAndReturns202_whenAuctionIsOpen() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(auctionId);
        when(auctionService.getAuctionEntity(auctionId)).thenReturn(auction);

        PlaceBidRequest request = new PlaceBidRequest("alice", new BigDecimal("150.00"));
        BidSubmittedResponse response = bidService.placeBid(auctionId, request);

        assertThat(response.message()).isNotBlank();
        assertThat(response.eventId()).isNotNull();

        ArgumentCaptor<BidEvent> captor = ArgumentCaptor.forClass(BidEvent.class);
        verify(bidProducer).publish(captor.capture());
        assertThat(captor.getValue().auctionId()).isEqualTo(auctionId);
        assertThat(captor.getValue().userId()).isEqualTo("alice");
        assertThat(captor.getValue().amount()).isEqualByComparingTo("150.00");
    }

    @Test
    void placeBid_throwsAuctionNotOpenException_whenAuctionIsScheduled() {
        UUID auctionId = UUID.randomUUID();
        when(auctionService.getAuctionEntity(auctionId)).thenReturn(TestUtils.scheduledAuction(auctionId));

        assertThatThrownBy(() -> bidService.placeBid(auctionId, new PlaceBidRequest("alice", new BigDecimal("150.00"))))
                .isInstanceOf(AuctionNotOpenException.class);

        verify(bidProducer, never()).publish(any());
    }

    @Test
    void placeBid_throwsAuctionNotOpenException_whenAuctionIsClosed() {
        UUID auctionId = UUID.randomUUID();
        when(auctionService.getAuctionEntity(auctionId)).thenReturn(TestUtils.closedAuction(auctionId));

        assertThatThrownBy(() -> bidService.placeBid(auctionId, new PlaceBidRequest("alice", new BigDecimal("150.00"))))
                .isInstanceOf(AuctionNotOpenException.class);

        verify(bidProducer, never()).publish(any());
    }

    @Test
    void placeBid_eventIdMatchesResponseEventId() {
        UUID auctionId = UUID.randomUUID();
        when(auctionService.getAuctionEntity(auctionId)).thenReturn(TestUtils.openAuction(auctionId));

        BidSubmittedResponse response = bidService.placeBid(auctionId, new PlaceBidRequest("bob", new BigDecimal("200.00")));

        ArgumentCaptor<BidEvent> captor = ArgumentCaptor.forClass(BidEvent.class);
        verify(bidProducer).publish(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(response.eventId());
    }
}
