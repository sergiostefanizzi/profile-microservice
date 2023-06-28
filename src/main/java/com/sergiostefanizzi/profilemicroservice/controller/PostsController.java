package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.PostsApi;
import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.PostPatch;
import com.sergiostefanizzi.profilemicroservice.service.PostsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PostsController implements PostsApi {
    private final PostsService postsService;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return PostsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<Post> addPost(Post post) {
        Post savedPost = this.postsService.save(post);
        return new ResponseEntity<>(savedPost, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deletePostById(Long postId) {
        this.postsService.remove(postId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Post> updatePostById(Long postId, PostPatch postPatch) {
        Post updatedPost = this.postsService.update(postId, postPatch);
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Post> findPostById(Long postId) {
        return PostsApi.super.findPostById(postId);
    }


    //TODO dopo aver fatto i follower
    @Override
    public ResponseEntity<List<Post>> profileFeedById(Long profileId) {
        return PostsApi.super.profileFeedById(profileId);
    }


}
