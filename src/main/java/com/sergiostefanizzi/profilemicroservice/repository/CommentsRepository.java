package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.CommentJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentsRepository extends JpaRepository<CommentJpa, Long> {
    @Query("SELECT c FROM CommentJpa c WHERE c.id=:commentId AND c.deletedAt IS NULL")
    Optional<CommentJpa> findActiveById(Long commentId);

    @Query("SELECT c.id FROM CommentJpa c WHERE c.id=:commentId AND c.deletedAt IS NULL")
    Optional<Long> checkActiveById(Long commentId);

    @Query("SELECT c FROM CommentJpa c WHERE c.post.id = :postId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<CommentJpa> findAllActiveByPostId(Long postId);


}
