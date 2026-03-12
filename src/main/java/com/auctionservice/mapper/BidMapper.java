package com.auctionservice.mapper;

import com.auctionservice.model.BidResponse;
import com.auctionservice.entity.Bid;
import org.springframework.stereotype.Component;

@Component
public class BidMapper {

    public BidResponse toResponse(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuctionId(),
                bid.getUserId(),
                bid.getAmount(),
                bid.getCreatedAt()
        );
    }
}
