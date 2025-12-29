package io.kemalthes.vpnservice.repository;

import io.kemalthes.vpnservice.dto.CommentResponse;
import io.kemalthes.vpnservice.entity.Comment;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends CrudRepository<Comment, Integer> {

    boolean existsByUserId(UUID userId);

    @Query("select avg(score) from comments")
    Double getAverageScore();

    @Query("""
        select c.text,
               c.score,
               c.created_at,
               u.id as user_id,
               u.name as user_name
        from comments c
        join users u on user_id = u.id
        where score > :minScore and length(text) > :minLength
        order by random()
        limit :limit
        """)
    List<CommentResponse> findRandomComments(
            @Param("minScore") double minScore,
            @Param("minLength") int minLength,
            @Param("limit") int limit
    );

    @Query("""
        select c.text,
               c.score,
               c.created_at,
               u.id as user_id,
               u.name as user_name
        from comments c
        join users u on c.user_id = u.id
        order by c.created_at desc
        limit :size
        offset :offset
        """)
    List<CommentResponse> findAllWithUser(
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query("""
        select c.text,
               c.score,
               c.created_at,
               u.id as user_id,
               u.name as user_name
        from comments c
        join users u on c.user_id = u.id
                where c.user_id = :userId
        limit 1
        """)
        CommentResponse findCommentByUserId(@Param("userId") UUID userId);
}
