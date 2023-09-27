package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostsRepository extends JpaRepository<PostJpa, Long> {

    String storyPostType = "STORY";
    String postPostType = "POST";

    @Query("SELECT p FROM PostJpa p WHERE p.profile.id=:profileId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Optional<List<PostJpa>> findAllActiveByProfileId(Long profileId);

    @Query("SELECT p FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL")
    Optional<PostJpa> findActiveById(Long postId);

    @Query("SELECT p.id FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL")
    Optional<Long> checkActiveById(Long postId);

    //questo mi serve perchè l'utente deve poter eliminare un storia scaduta
    @Query("SELECT p.id FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL")
    Optional<Long> checkActiveForDeleteById(Long postId);

    @Query("SELECT p FROM PostJpa p INNER JOIN FollowsJpa f ON p.profile.id = f.followsId.followedId WHERE f.followsId.followerId = :profileId AND p.postType = '"+postPostType+"' AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PostJpa> getPostFeedByProfileId(Long profileId);

    @Query("SELECT p FROM PostJpa p INNER JOIN FollowsJpa f ON p.profile.id = f.followsId.followedId WHERE f.followsId.followerId = :profileId AND p.postType = '"+storyPostType+"' AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PostJpa> getStoryFeedByProfileId(Long profileId);

    @Query("SELECT p FROM PostJpa p INNER JOIN FollowsJpa f ON p.profile.id = f.followsId.followedId WHERE f.followsId.followerId = :profileId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PostJpa> getFeedByProfileId(Long profileId);

    @Query("SELECT p.id FROM PostJpa p INNER JOIN CommentJpa c ON p.id = c.post.id WHERE c.id=:commentId AND p.deletedAt IS NULL")
    Optional<Long> checkActiveByCommentId(Long commentId);

    @Query("SELECT p FROM PostJpa p WHERE (p.deletedAt IS NULL AND (p.postType = '"+storyPostType+"' AND p.createdAt < :timeLimit))")
    List<PostJpa> getOutdatedStories(LocalDateTime timeLimit);
}
