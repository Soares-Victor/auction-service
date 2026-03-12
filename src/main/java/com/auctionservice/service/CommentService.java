package com.auctionservice.service;

import com.auctionservice.model.CommentResponse;
import com.auctionservice.model.CreateCommentRequest;
import com.auctionservice.mapper.CommentMapper;
import com.auctionservice.entity.Comment;
import com.auctionservice.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CommentService {

    private final AuctionService auctionService;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    public CommentService(AuctionService auctionService,
                          CommentRepository commentRepository,
                          CommentMapper commentMapper) {
        this.auctionService = auctionService;
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
    }

    @Transactional
    public CommentResponse addComment(UUID auctionId, CreateCommentRequest request) {
        auctionService.getAuctionEntity(auctionId);

        Comment comment = new Comment();
        comment.setAuctionId(auctionId);
        comment.setUserId(request.userId());
        comment.setContent(request.content());
        commentRepository.save(comment);

        return commentMapper.toResponse(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> listComments(UUID auctionId, Pageable pageable) {
        auctionService.getAuctionEntity(auctionId);
        return commentRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId, pageable)
                .map(commentMapper::toResponse);
    }
}
