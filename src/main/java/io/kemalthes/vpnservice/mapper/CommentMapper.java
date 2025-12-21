package io.kemalthes.vpnservice.mapper;

import io.kemalthes.vpnservice.dto.CommentResponse;
import io.kemalthes.vpnservice.entity.Comment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    List<CommentResponse> toDtoList(List<Comment> comment);

}
