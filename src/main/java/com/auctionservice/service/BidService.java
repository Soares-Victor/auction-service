package com.auctionservice.service;

import com.auctionservice.model.BidResponse;
import com.auctionservice.model.BidSubmittedResponse;
import com.auctionservice.model.PlaceBidRequest;
import com.auctionservice.exception.AuctionNotOpenException;
import com.auctionservice.kafka.BidEvent;
import com.auctionservice.kafka.BidProducer;
import com.auctionservice.mapper.BidMapper;
import com.auctionservice.entity.Auction;
import com.auctionservice.entity.AuctionStatus;
import com.auctionservice.repository.BidRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BidService {

    private final AuctionService auctionService;
    private final BidRepository bidRepository;
    private final BidProducer bidProducer;
    private final BidMapper bidMapper;

    public BidService(AuctionService auctionService,
                      BidRepository bidRepository,
                      BidProducer bidProducer,
                      BidMapper bidMapper) {
        this.auctionService = auctionService;
        this.bidRepository = bidRepository;
        this.bidProducer = bidProducer;
        this.bidMapper = bidMapper;
    }

    public BidSubmittedResponse placeBid(UUID auctionId, PlaceBidRequest request) {
        Auction auction = auctionService.getAuctionEntity(auctionId);

        // Optimistic fast-fail: reject obviously invalid bids before queuing.
        // The consumer will re-validate against the authoritative state at processing time,
        // since many other bids may arrive between now and then.
        if (AuctionStatus.compute(auction.getStartTime(), auction.getEndTime()) != AuctionStatus.OPEN) {
            throw new AuctionNotOpenException("Auction is not open for bidding. Current status: "
                    + AuctionStatus.compute(auction.getStartTime(), auction.getEndTime()));
        }

        UUID eventId = UUID.randomUUID();
        BidEvent event = new BidEvent(eventId, auctionId, request.userId(), request.amount(), Instant.now());
        bidProducer.publish(event);

        return new BidSubmittedResponse("Bid submitted for processing", eventId);
    }

    @Transactional(readOnly = true)
    public Page<BidResponse> getBidHistory(UUID auctionId, Pageable pageable) {
        auctionService.getAuctionEntity(auctionId);
        return bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId, pageable)
                .map(bidMapper::toResponse);
    }
}
