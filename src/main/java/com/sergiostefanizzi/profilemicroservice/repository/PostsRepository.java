package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostsRepository extends JpaRepository<PostJpa, Long> {

    @Query("SELECT p FROM PostJpa p WHERE p.profile.id=:profileId AND p.deletedAt IS NULL")
    Optional<List<PostJpa>> findAllByProfileId(Long profileId);

    @Query("SELECT p FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL")
    Optional<PostJpa> findActiveById(Long postId);

    @Query("SELECT p.id FROM PostJpa p WHERE p.id=:postId AND p.deletedAt IS NULL")
    Optional<Long> checkActiveById(Long postId);
}
