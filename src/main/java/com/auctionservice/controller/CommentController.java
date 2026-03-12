package com.auctionservice.controller;

import com.auctionservice.model.CommentResponse;
import com.auctionservice.model.CreateCommentRequest;
import com.auctionservice.service.CommentService;
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
@RequestMapping("/auctions/{auctionId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable UUID auctionId,
                                       @Valid @RequestBody CreateCommentRequest request) {
        return commentService.addComment(auctionId, request);
    }

    @GetMapping
    public Page<CommentResponse> listComments(@PathVariable UUID auctionId,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return commentService.listComments(auctionId, pageable);
    }
}
