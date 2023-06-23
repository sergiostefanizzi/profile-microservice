package com.sergiostefanizzi.profilemicroservice.model.converter;

import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PostToPostJpaConverter implements Converter<Post, PostJpa> {

    @Override
    public PostJpa convert(Post source) {
        PostJpa postJpa = new PostJpa(
                source.getContentUrl(),
                source.getPostType());
        postJpa.setCaption(source.getCaption());
        // set del Profile nel service
        return postJpa;
    }

    public Post convertBack(PostJpa source){
        Post post = new Post(source.getContentUrl(),
                source.getPostType(),
                source.getProfile().getId());
        post.setCaption(source.getCaption());
        post.setId(source.getId());
        return post;
    }
}
