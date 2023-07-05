package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.LikeId;
import com.sergiostefanizzi.profilemicroservice.model.LikeJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikesRepository extends JpaRepository<LikeJpa, LikeId> {
}
