package io.nesvpn.backendsiteservice.controller;

import io.nesvpn.backendsiteservice.dto.CommentRequest;
import io.nesvpn.backendsiteservice.dto.CommentResponse;
import io.nesvpn.backendsiteservice.dto.CommentsPageResponse;
import io.nesvpn.backendsiteservice.exception.Swagger403;
import io.nesvpn.backendsiteservice.exception.Swagger404;
import io.nesvpn.backendsiteservice.exception.Swagger409;
import io.nesvpn.backendsiteservice.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@Tag(name = "Comment page", description = "Страница отзывов")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentService service;

    @GetMapping(produces = "application/json")
    public CommentsPageResponse getCommentsPage(@PageableDefault Pageable pageable) {
        return service.findAll(pageable.getPageNumber(), pageable.getPageSize());
    }

    @Swagger404
    @GetMapping(value = "/users/{uuid}", produces = "application/json")
    public CommentResponse getCurrentUserComment(@PathVariable UUID uuid) {
        return service.findById(uuid);
    }

    @Swagger404
    @GetMapping(value = "/users/{uuid}/has-order", produces = "application/json")
    public boolean isHasOrder(@PathVariable UUID uuid) {
        return service.hasOrder(uuid);
    }

    @Swagger403 @Swagger404 @Swagger409
    @PostMapping
    public void save(@RequestBody @Valid CommentRequest commentRequest) {
        service.create(commentRequest);
    }

    @Swagger404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
