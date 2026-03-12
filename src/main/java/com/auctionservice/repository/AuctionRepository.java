package com.auctionservice.repository;

import com.auctionservice.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    Page<Auction> findAll(Pageable pageable);

    List<Auction> findByEndTimeBetween(Instant from, Instant to);
}
