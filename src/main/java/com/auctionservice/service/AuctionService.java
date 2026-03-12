package com.auctionservice.service;

import com.auctionservice.model.AuctionResponse;
import com.auctionservice.model.CreateAuctionRequest;
import com.auctionservice.exception.ResourceNotFoundException;
import com.auctionservice.mapper.AuctionMapper;
import com.auctionservice.entity.Auction;
import com.auctionservice.entity.Bid;
import com.auctionservice.repository.AuctionRepository;
import com.auctionservice.repository.BidRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionMapper auctionMapper;

    public AuctionService(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          AuctionMapper auctionMapper) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.auctionMapper = auctionMapper;
    }

    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest request) {
        // Per spec: "When a user puts an item up for auction they set a start and end time in the future"
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (!request.startTime().isAfter(Instant.now())) {
            throw new IllegalArgumentException("startTime must be in the future");
        }

        Auction auction = new Auction();
        auction.setSellerId(request.sellerId());
        auction.setTitle(request.title());
        auction.setDescription(request.description());
        auction.setStartTime(request.startTime());
        auction.setEndTime(request.endTime());
        auction.setStartingPrice(request.startingPrice());
        auction.setMinBidIncrement(request.minBidIncrement());
        auctionRepository.save(auction);

        return auctionMapper.toResponse(auction, null);
    }

    @Transactional(readOnly = true)
    public Page<AuctionResponse> listAuctions(Pageable pageable) {
        Pageable sortedByStartTime = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "startTime")
        );

        Page<Auction> auctionPage = auctionRepository.findAll(sortedByStartTime);

        // Fetch top bids for all auctions on this page in a single query — avoids N+1
        List<UUID> auctionIds = auctionPage.map(Auction::getId).toList();
        Map<UUID, Bid> topBidByAuction = bidRepository.findTopBidsByAuctionIds(auctionIds).stream()
                .collect(Collectors.toMap(Bid::getAuctionId, Function.identity(), (a, b) -> a));

        return auctionPage.map(auction -> auctionMapper.toResponse(auction, topBidByAuction.get(auction.getId())));
    }

    @Transactional(readOnly = true)
    public AuctionResponse getAuction(UUID id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));
        Bid topBid = bidRepository.findTopByAuctionIdOrderByAmountDesc(id).orElse(null);
        return auctionMapper.toResponse(auction, topBid);
    }

    public Auction getAuctionEntity(UUID id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));
    }
}
