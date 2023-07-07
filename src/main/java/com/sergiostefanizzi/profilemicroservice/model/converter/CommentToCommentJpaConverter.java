package com.sergiostefanizzi.profilemicroservice.model.converter;

import com.sergiostefanizzi.profilemicroservice.model.Comment;
import com.sergiostefanizzi.profilemicroservice.model.CommentJpa;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class CommentToCommentJpaConverter implements Converter<Comment, CommentJpa> {
    @Override
    public CommentJpa convert(Comment source) {
        // Profile e Post da settare nel service
        return new CommentJpa(source.getContent());
    }

    public Comment convertBack(CommentJpa source){
        Comment comment = new Comment(
                source.getProfile().getId(),
                source.getPost().getId(),
                source.getContent()
        );
        comment.setId(source.getId());
        return comment;
    }
}
