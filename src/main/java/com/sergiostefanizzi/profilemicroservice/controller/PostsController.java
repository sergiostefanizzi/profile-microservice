package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.PostsApi;
import com.sergiostefanizzi.profilemicroservice.model.*;
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
        Post post = this.postsService.find(postId);
        return new ResponseEntity<>(post, HttpStatus.OK);
    }


    //TODO dopo aver fatto i follower
    @Override
    public ResponseEntity<List<Post>> profileFeedById(Long profileId) {
        return PostsApi.super.profileFeedById(profileId);
    }

    @Override
    public ResponseEntity<Void> addLike(Boolean removeLike, Like like) {
        this.postsService.addLike(removeLike, like);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<List<Like>> findAllLikesByPostId(Long postId) {
        List<Like> likeList = this.postsService.findAllLikesByPostId(postId);
        return new ResponseEntity<>(likeList, HttpStatus.OK);
    }
    @Override
    public ResponseEntity<Comment> addComment(Comment comment) {
        Comment savedComment = this.postsService.addComment(comment);
        return new ResponseEntity<>(savedComment, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Comment> updateCommentById(Long commentId, CommentPatch commentPatch) {
        Comment updatedComment = this.postsService.updateCommentById(commentId, commentPatch);
        return new ResponseEntity<>(updatedComment, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Comment> deleteCommentById(Long commentId) {
        return PostsApi.super.deleteCommentById(commentId);
    }

    @Override
    public ResponseEntity<List<Comment>> findAllCommentsById(Long postId) {
        return PostsApi.super.findAllCommentsById(postId);
    }
}
