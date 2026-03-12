package com.auctionservice.controller;

import com.auctionservice.model.AuctionResponse;
import com.auctionservice.model.CreateAuctionRequest;
import com.auctionservice.service.AuctionService;
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
@RequestMapping("/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping
    public Page<AuctionResponse> listAuctions(@PageableDefault(size = 20) Pageable pageable) {
        return auctionService.listAuctions(pageable);
    }

    @GetMapping("/{id}")
    public AuctionResponse getAuction(@PathVariable UUID id) {
        return auctionService.getAuction(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        return auctionService.createAuction(request);
    }
}
