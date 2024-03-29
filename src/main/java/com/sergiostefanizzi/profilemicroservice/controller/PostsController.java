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
    public ResponseEntity<Post> addPost(Post post) {
        Post savedPost = this.postsService.save(post);
        return new ResponseEntity<>(savedPost, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deletePostById(Long postId, Long selectedUserProfileId) {
        this.postsService.remove(postId, selectedUserProfileId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Post> updatePostById(Long postId, Long selectedUserProfileId, PostPatch postPatch) {
        Post updatedPost = this.postsService.update(postId, selectedUserProfileId, postPatch);
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Post> findPostById(Long postId, Long selectedUserProfileId) {
        Post post = this.postsService.find(postId, selectedUserProfileId);
        return new ResponseEntity<>(post, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Post>> profileFeedByProfileId(Long profileId,Long selectedUserProfileId, Boolean onlyPost) {
        List<Post> postList = this.postsService.profileFeedByProfileId(profileId, selectedUserProfileId, onlyPost);
        return new ResponseEntity<>(postList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> addLike(Boolean removeLike, Like like) {
        this.postsService.addLike(removeLike, like);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<List<Like>> findAllLikesByPostId(Long postId, Long selectedUserProfileId) {
        List<Like> likeList = this.postsService.findAllLikesByPostId(postId, selectedUserProfileId);
        return new ResponseEntity<>(likeList, HttpStatus.OK);
    }
    @Override
    public ResponseEntity<Comment> addComment(Comment comment) {
        Comment savedComment = this.postsService.addComment(comment);
        return new ResponseEntity<>(savedComment, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Comment> updateCommentById(Long commentId, Long selectedUserProfileId, CommentPatch commentPatch) {
        Comment updatedComment = this.postsService.updateCommentById(commentId, selectedUserProfileId, commentPatch);
        return new ResponseEntity<>(updatedComment, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteCommentById(Long commentId, Long selectedUserProfileId) {
        this.postsService.deleteCommentById(commentId, selectedUserProfileId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<List<Comment>> findAllCommentsByPostId(Long postId,  Long selectedUserProfileId) {
        List<Comment> commentList = this.postsService.findAllCommentsByPostId(postId, selectedUserProfileId);
        return new ResponseEntity<>(commentList, HttpStatus.OK);
    }
}
