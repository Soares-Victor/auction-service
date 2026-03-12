package com.auctionservice.repository;

import com.auctionservice.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByAuctionIdOrderByCreatedAtAsc(UUID auctionId, Pageable pageable);
}
