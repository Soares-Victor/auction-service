package com.auctionservice.mapper;

import com.auctionservice.model.AuctionResponse;
import com.auctionservice.entity.Auction;
import com.auctionservice.entity.AuctionStatus;
import com.auctionservice.entity.Bid;
import org.springframework.stereotype.Component;

@Component
public class AuctionMapper {

    public AuctionResponse toResponse(Auction auction, Bid topBid) {
        return new AuctionResponse(
                auction.getId(),
                auction.getSellerId(),
                auction.getTitle(),
                auction.getDescription(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStartingPrice(),
                auction.getMinBidIncrement(),
                AuctionStatus.compute(auction.getStartTime(), auction.getEndTime()),
                topBid != null ? topBid.getAmount() : null,
                topBid != null ? topBid.getUserId() : null,
                auction.getCreatedAt()
        );
    }
}
