package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostsRepository extends JpaRepository<PostJpa, Long> {

    @Query("SELECT p FROM PostJpa p WHERE p.profile.id=:profileId AND p.deletedAt IS NULL AND ((p.postType = 'POST') OR (p.postType = 'STORY' AND p.createdAt >= :timeLimit)) ORDER BY p.createdAt DESC")
    Optional<List<PostJpa>> findAllActiveByProfileId(Long profileId, LocalDateTime timeLimit);

    @Query("SELECT p FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL AND ((p.postType = 'POST') OR (p.postType = 'STORY' AND p.createdAt >= :timeLimit))")
    Optional<PostJpa> findActiveById(Long postId, LocalDateTime timeLimit);

    @Query("SELECT p.id FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL AND ((p.postType = 'POST') OR (p.postType = 'STORY' AND p.createdAt >= :timeLimit))")
    Optional<Long> checkActiveById(Long postId, LocalDateTime timeLimit);

    //questo mi serve perchè l'utente deve poter eliminare un storia scaduta
    @Query("SELECT p.id FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL")
    Optional<Long> checkActiveForDeleteById(Long postId);

    @Query("SELECT p FROM PostJpa p INNER JOIN FollowsJpa f ON p.profile.id = f.followsId.followedId WHERE f.followsId.followerId = :profileId AND p.postType = 'POST' AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PostJpa> getPostFeedByProfileId(Long profileId);

    @Query("SELECT p FROM PostJpa p INNER JOIN FollowsJpa f ON p.profile.id = f.followsId.followedId WHERE f.followsId.followerId = :profileId AND p.postType = 'STORY' AND p.deletedAt IS NULL AND p.createdAt >= :timeLimit ORDER BY p.createdAt DESC")
    List<PostJpa> getStoryFeedByProfileId(Long profileId, LocalDateTime timeLimit);

    @Query("SELECT p FROM PostJpa p INNER JOIN FollowsJpa f ON p.profile.id = f.followsId.followedId WHERE f.followsId.followerId = :profileId AND p.deletedAt IS NULL AND ((p.postType = 'POST') OR (p.postType = 'STORY' AND p.createdAt >= :timeLimit)) ORDER BY p.createdAt DESC")
    List<PostJpa> getFeedByProfileId(Long profileId, LocalDateTime timeLimit);

    @Modifying
    @Query("UPDATE PostJpa p SET p.deletedAt = :removalDate WHERE p.profile.id = :profileId")
    void removePostByProfileId(Long profileId, LocalDateTime removalDate);

    @Modifying
    @Query("UPDATE PostJpa p SET p.deletedAt = :removalDate WHERE p.id = :postId")
    void removePostByPostId(Long postId, LocalDateTime removalDate);
}
