package com.auctionservice.mapper;

import com.auctionservice.model.CommentResponse;
import com.auctionservice.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuctionId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
