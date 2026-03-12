package com.auctionservice.service;

import com.auctionservice.entity.Auction;
import com.auctionservice.entity.Bid;
import com.auctionservice.repository.AuctionRepository;
import com.auctionservice.repository.BidRepository;
import com.auctionservice.websocket.AuctionClosedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class AuctionClosingService {

    private static final Logger log = LoggerFactory.getLogger(AuctionClosingService.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionClosingService(AuctionRepository auctionRepository,
                                  BidRepository bidRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts AUCTION_CLOSED messages to all clients subscribed to the auctions topic every 30 seconds.
     * This is just a notification, and the auction will be closed automatically after the timeout.
     */
    @Scheduled(fixedRate = 30_000)
    public void broadcastJustClosedAuctions() {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(30);

        List<Auction> justClosed = auctionRepository.findByEndTimeBetween(windowStart, now);
        for (Auction auction : justClosed) {
            Optional<Bid> winner = bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAsc(auction.getId());
            AuctionClosedMessage message = new AuctionClosedMessage(
                    "AUCTION_CLOSED",
                    auction.getId(),
                    winner.map(Bid::getUserId).orElse(null),
                    winner.map(Bid::getAmount).orElse(null),
                    auction.getEndTime()
            );
            messagingTemplate.convertAndSend("/topic/auctions/" + auction.getId(), message);
            log.info("Broadcast AUCTION_CLOSED for auction {} — winner: {}", auction.getId(),
                    winner.map(Bid::getUserId).orElse("none"));
        }
    }
}
