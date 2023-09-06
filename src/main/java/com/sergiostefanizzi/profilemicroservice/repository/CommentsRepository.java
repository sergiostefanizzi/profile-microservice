package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.CommentJpa;
import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentsRepository extends JpaRepository<CommentJpa, Long> {
    @Query("SELECT c FROM CommentJpa c WHERE c.id=:commentId AND c.deletedAt IS NULL")
    Optional<CommentJpa> findActiveById(Long commentId);

    @Query("SELECT c.id FROM CommentJpa c WHERE c.id=:commentId AND c.deletedAt IS NULL")
    Optional<Long> checkActiveById(Long commentId);

    @Query("SELECT c FROM CommentJpa c WHERE c.post = :post AND c.deletedAt IS NULL")
    List<CommentJpa> findAllActiveByPost(PostJpa post);
}
