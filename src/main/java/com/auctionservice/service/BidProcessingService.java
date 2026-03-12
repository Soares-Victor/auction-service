package com.auctionservice.service;

import com.auctionservice.kafka.BidEvent;
import com.auctionservice.entity.Auction;
import com.auctionservice.entity.AuctionStatus;
import com.auctionservice.entity.Bid;
import com.auctionservice.repository.AuctionRepository;
import com.auctionservice.repository.BidRepository;
import com.auctionservice.websocket.BidPlacedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BidProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BidProcessingService.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public BidProcessingService(AuctionRepository auctionRepository,
                                 BidRepository bidRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public boolean process(BidEvent event) {
        Auction auction = auctionRepository.findById(event.auctionId()).orElse(null);
        if (auction == null) {
            log.warn("Bid event {} discarded: auction {} not found", event.eventId(), event.auctionId());
            return false;
        }

        if (!isAuctionOpen(auction)) {
            log.info("Bid event {} discarded: auction {} is not OPEN", event.eventId(), event.auctionId());
            return false;
        }

        Bid currentLeader = bidRepository.findTopByAuctionIdOrderByAmountDesc(auction.getId()).orElse(null);
        if (currentLeader != null && currentLeader.getUserId().equals(event.userId())) {
            log.info("Bid event {} discarded: user {} is already the current leader on auction {}",
                    event.eventId(), event.userId(), event.auctionId());
            return false;
        }

        BigDecimal minValidAmount = resolveMinValidAmount(auction, currentLeader);
        if (!isBidAmountSufficient(event.amount(), minValidAmount)) {
            log.info("Bid event {} discarded: amount {} is below required minimum {}",
                    event.eventId(), event.amount(), minValidAmount);
            return false;
        }

        Bid bid = persistBid(event);
        broadcastBidPlaced(bid, event);

        return true;
    }

    private boolean isAuctionOpen(Auction auction) {
        return AuctionStatus.compute(auction.getStartTime(), auction.getEndTime()) == AuctionStatus.OPEN;
    }

    private BigDecimal resolveMinValidAmount(Auction auction, Bid currentLeader) {
        return currentLeader != null
                ? currentLeader.getAmount().add(auction.getMinBidIncrement())
                : auction.getStartingPrice();
    }

    private boolean isBidAmountSufficient(BigDecimal amount, BigDecimal minValidAmount) {
        return amount.compareTo(minValidAmount) >= 0;
    }

    private Bid persistBid(BidEvent event) {
        Bid bid = new Bid();
        bid.setAuctionId(event.auctionId());
        bid.setUserId(event.userId());
        bid.setAmount(event.amount());
        return bidRepository.save(bid);
    }

    private void broadcastBidPlaced(Bid bid, BidEvent event) {
        BidPlacedMessage message = new BidPlacedMessage(
                "BID_PLACED",
                event.auctionId(),
                bid.getId(),
                event.userId(),
                event.amount(),
                event.publishedAt()
        );
        messagingTemplate.convertAndSend("/topic/auctions/" + event.auctionId(), message);
    }
}
