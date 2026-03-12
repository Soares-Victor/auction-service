package com.auctionservice.controller;

import com.auctionservice.model.BidResponse;
import com.auctionservice.model.BidSubmittedResponse;
import com.auctionservice.model.PlaceBidRequest;
import com.auctionservice.service.BidService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auctions/{auctionId}/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BidSubmittedResponse placeBid(@PathVariable UUID auctionId,
                                         @Valid @RequestBody PlaceBidRequest request) {
        return bidService.placeBid(auctionId, request);
    }

    @GetMapping
    public Page<BidResponse> getBidHistory(@PathVariable UUID auctionId,
                                           @PageableDefault(size = 50) Pageable pageable) {
        return bidService.getBidHistory(auctionId, pageable);
    }
}
