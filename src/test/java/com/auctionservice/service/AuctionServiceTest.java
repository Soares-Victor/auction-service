package com.auctionservice.service;

import com.auctionservice.TestUtils;
import com.auctionservice.entity.Auction;
import com.auctionservice.entity.AuctionStatus;
import com.auctionservice.entity.Bid;
import com.auctionservice.exception.ResourceNotFoundException;
import com.auctionservice.mapper.AuctionMapper;
import com.auctionservice.model.AuctionResponse;
import com.auctionservice.model.CreateAuctionRequest;
import com.auctionservice.repository.AuctionRepository;
import com.auctionservice.repository.BidRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock AuctionRepository auctionRepository;
    @Mock BidRepository     bidRepository;
    @Mock AuctionMapper     auctionMapper;

    @InjectMocks AuctionService auctionService;

    @Test
    void createAuction_throwsIllegalArgument_whenEndTimeBeforeStartTime() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end   = Instant.now().plusSeconds(1800); // before start

        assertThatThrownBy(() -> auctionService.createAuction(request(start, end)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime");
    }

    @Test
    void createAuction_throwsIllegalArgument_whenStartTimeIsInThePast() {
        Instant start = Instant.now().minusSeconds(60);
        Instant end   = Instant.now().plusSeconds(3600);

        assertThatThrownBy(() -> auctionService.createAuction(request(start, end)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startTime");
    }

    @Test
    void createAuction_savesAndReturnsResponse_whenValid() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end   = Instant.now().plusSeconds(7200);
        AuctionResponse expectedResponse = dummyResponse();

        when(auctionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auctionMapper.toResponse(any(Auction.class), any())).thenReturn(expectedResponse);

        AuctionResponse response = auctionService.createAuction(request(start, end));

        assertThat(response).isEqualTo(expectedResponse);
        verify(auctionRepository).save(any(Auction.class));
    }

    @Test
    void getAuction_throwsResourceNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(auctionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionService.getAuction(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAuction_returnsResponseWithTopBid_whenFound() {
        UUID id = UUID.randomUUID();
        Auction auction = TestUtils.openAuction(id);
        Bid topBid = TestUtils.bid(UUID.randomUUID(), id, "alice", new BigDecimal("200.00"));
        AuctionResponse expectedResponse = dummyResponse();

        when(auctionRepository.findById(id)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDesc(id)).thenReturn(Optional.of(topBid));
        when(auctionMapper.toResponse(auction, topBid)).thenReturn(expectedResponse);

        AuctionResponse response = auctionService.getAuction(id);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void listAuctions_fetchesTopBidsInSingleQuery_avoidingNPlusOne() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Auction a1 = TestUtils.openAuction(id1);
        Auction a2 = TestUtils.openAuction(id2);
        Bid bid1 = TestUtils.bid(UUID.randomUUID(), id1, "alice", new BigDecimal("200.00"));

        Pageable pageable = PageRequest.of(0, 20);
        when(auctionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(a1, a2)));
        when(bidRepository.findTopBidsByAuctionIds(List.of(id1, id2))).thenReturn(List.of(bid1));
        when(auctionMapper.toResponse(any(Auction.class), any())).thenReturn(dummyResponse());

        auctionService.listAuctions(pageable);

        // Top bids fetched in ONE call for both auctions, not one per auction
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(bidRepository).findTopBidsByAuctionIds(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(id1, id2);
    }

    private CreateAuctionRequest request(Instant start, Instant end) {
        return new CreateAuctionRequest(
                "seller1", "Test Item", "Description",
                start, end,
                new BigDecimal("100.00"), new BigDecimal("10.00")
        );
    }

    private AuctionResponse dummyResponse() {
        UUID id = UUID.randomUUID();
        return new AuctionResponse(
                id, "seller1", "Test Item", "Description",
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600),
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                AuctionStatus.OPEN, null, null, Instant.now()
        );
    }
}
