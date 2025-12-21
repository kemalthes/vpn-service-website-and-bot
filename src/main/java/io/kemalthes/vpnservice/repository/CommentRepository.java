package io.kemalthes.vpnservice.repository;

import io.kemalthes.vpnservice.entity.Comment;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, Integer> {

    @Query("select avg(score) from comments")
    Double getAverageScore();

    @Query("""
        select * from comments
        where score > :minScore and length(text) > :minLength
        order by random()
        limit :limit
        """)
    List<Comment> getRandomCommentsByScoreAndTextLength(
            @Param("minScore") double minScore,
            @Param("minLength") int minLength,
            @Param("limit") int limit
    );
}
