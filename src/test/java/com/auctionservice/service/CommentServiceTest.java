package com.auctionservice.service;

import com.auctionservice.TestUtils;
import com.auctionservice.entity.Comment;
import static org.mockito.ArgumentMatchers.any;
import com.auctionservice.exception.ResourceNotFoundException;
import com.auctionservice.mapper.CommentMapper;
import com.auctionservice.model.CommentResponse;
import com.auctionservice.model.CreateCommentRequest;
import com.auctionservice.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock AuctionService    auctionService;
    @Mock CommentRepository commentRepository;
    @Mock CommentMapper     commentMapper;

    @InjectMocks CommentService commentService;

    @Test
    void addComment_savesAndReturnsResponse() {
        UUID auctionId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment saved = TestUtils.comment(commentId, auctionId, "bob", "Great item!");
        CommentResponse expectedResponse = new CommentResponse(commentId, auctionId, "bob", "Great item!", Instant.now());

        when(commentRepository.save(any())).thenReturn(saved);
        when(commentMapper.toResponse(any(Comment.class))).thenReturn(expectedResponse);

        CommentResponse response = commentService.addComment(auctionId, new CreateCommentRequest("bob", "Great item!"));

        assertThat(response).isEqualTo(expectedResponse);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("bob");
        assertThat(captor.getValue().getContent()).isEqualTo("Great item!");
        assertThat(captor.getValue().getAuctionId()).isEqualTo(auctionId);
    }

    @Test
    void addComment_throwsResourceNotFound_whenAuctionMissing() {
        UUID auctionId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Auction not found: " + auctionId))
                .when(auctionService).getAuctionEntity(auctionId);

        assertThatThrownBy(() -> commentService.addComment(auctionId, new CreateCommentRequest("bob", "Hello")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listComments_returnsMappedPage() {
        UUID auctionId = UUID.randomUUID();
        Comment comment = TestUtils.comment(UUID.randomUUID(), auctionId, "bob", "Nice!");
        CommentResponse mappedResponse = new CommentResponse(comment.getId(), auctionId, "bob", "Nice!", Instant.now());

        when(commentRepository.findByAuctionIdOrderByCreatedAtAsc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(commentMapper.toResponse(comment)).thenReturn(mappedResponse);

        var page = commentService.listComments(auctionId, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).content()).isEqualTo("Nice!");
    }
}
