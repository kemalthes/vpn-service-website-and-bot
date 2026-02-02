package io.nesvpn.backendsiteservice.service;

import io.nesvpn.backendsiteservice.dto.CommentRequest;
import io.nesvpn.backendsiteservice.dto.CommentResponse;
import io.nesvpn.backendsiteservice.dto.CommentsPageResponse;
import io.nesvpn.backendsiteservice.entity.Comment;
import io.nesvpn.backendsiteservice.enums.OrderStatus;
import io.nesvpn.backendsiteservice.exception.AlreadyExistsException;
import io.nesvpn.backendsiteservice.exception.ForbiddenException;
import io.nesvpn.backendsiteservice.exception.NotFoundException;
import io.nesvpn.backendsiteservice.repository.CommentRepository;
import io.nesvpn.backendsiteservice.repository.OrderRepository;
import io.nesvpn.backendsiteservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "comments")
public class CommentService {

    private final CommentRepository repository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Cacheable(key = "'page=' + #page + ',size=' + #size")
    public CommentsPageResponse findAll(int page, int size) {
        List<CommentResponse> comments = repository.findAllWithUser(size, page * size);
        long count = repository.count();
        int totalPages = (int) Math.ceil((double) count / size);
        CommentsPageResponse commentsPageResponse = new CommentsPageResponse();
        commentsPageResponse.setPage(page);
        commentsPageResponse.setSize(size);
        commentsPageResponse.setTotalPages(totalPages);
        commentsPageResponse.setComments(comments);
        commentsPageResponse.setCount(count);
        commentsPageResponse.setAverageScore(repository.getAverageScore());
        return commentsPageResponse;
    }

    @Cacheable(key = "'userId=' + #uuid", unless = "#result == null")
    public CommentResponse findById(UUID uuid) {
        if (!userRepository.existsById(uuid)) {
            throw new NotFoundException("Current user not found");
        }
        CommentResponse commentResponse = repository.findCommentByUserId(uuid);
        if (commentResponse == null) {
            throw new NotFoundException("Current user`s comment not found");
        }
        return commentResponse;
    }

    @Cacheable(key = "'hasOrder=' + #uuid")
    public boolean hasOrder(UUID uuid) {
        if (!userRepository.existsById(uuid)) {
            log.warn("User with id {} not found when checking for orders", uuid);
            return false;
        }
        return orderRepository.existsByUserIdAndStatus(uuid, OrderStatus.PAID.name().toLowerCase());
    }

    @CacheEvict(allEntries = true)
    public void create(CommentRequest commentRequest) {
        if (!userRepository.existsById(commentRequest.getUserId())) {
            throw new NotFoundException("Current user not found");
        }
        if (repository.existsByUserId(commentRequest.getUserId())) {
            throw new AlreadyExistsException("Current user's comment already exists");
        }
        if (!orderRepository.existsByUserIdAndStatus(commentRequest.getUserId(), OrderStatus.PAID.name().toLowerCase())) {
            throw new ForbiddenException("Not allowed to create comment");
        }
        repository.save(Comment.builder()
                .text(commentRequest.getText())
                .score(commentRequest.getScore())
                .userId(commentRequest.getUserId())
                .build()
        );
    }

    @CacheEvict(allEntries = true)
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Comment not found");
        }
        repository.deleteById(id);
    }
}
