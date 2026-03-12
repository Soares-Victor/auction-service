package com.auctionservice.repository;

import com.auctionservice.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {

    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(UUID auctionId);

    Page<Bid> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId, Pageable pageable);

    // FIFO winner: highest amount, earliest submission as tiebreaker
    Optional<Bid> findTopByAuctionIdOrderByAmountDescCreatedAtAsc(UUID auctionId);

    // Fetches the current highest bid for each auction in a single query — avoids N+1 in list endpoint
    @Query("SELECT b FROM Bid b WHERE b.auctionId IN :auctionIds AND b.amount = " +
           "(SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.auctionId = b.auctionId)")
    List<Bid> findTopBidsByAuctionIds(@Param("auctionIds") List<UUID> auctionIds);
}
