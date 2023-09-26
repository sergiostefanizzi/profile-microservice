package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.LikeId;
import com.sergiostefanizzi.profilemicroservice.model.LikeJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikesRepository extends JpaRepository<LikeJpa, LikeId> {

    @Query("SELECT l FROM LikeJpa l WHERE l.post.id = :postId AND l.deletedAt IS NULL")
    List<LikeJpa> findAllActiveByPostId(Long postId);

    @Query("SELECT l FROM LikeJpa l WHERE l.likeId = :likeId AND l.deletedAt IS NULL")
    Optional<LikeJpa> findActiveById(LikeId likeId);


}
