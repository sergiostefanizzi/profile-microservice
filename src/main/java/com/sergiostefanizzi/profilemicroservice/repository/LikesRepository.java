package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.LikeId;
import com.sergiostefanizzi.profilemicroservice.model.LikeJpa;
import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikesRepository extends JpaRepository<LikeJpa, LikeId> {

    @Query("SELECT l FROM LikeJpa l WHERE l.post = :post AND l.deletedAt IS NULL")
    List<LikeJpa> findAllActiveByPost(PostJpa post);
}
