package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.FollowsId;
import com.sergiostefanizzi.profilemicroservice.model.FollowsJpa;
import com.sergiostefanizzi.profilemicroservice.model.LikeJpa;
import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowsRepository extends JpaRepository<FollowsJpa, FollowsId> {
    @Query("SELECT f FROM FollowsJpa f WHERE f.id = :id AND f.requestStatus ='ACCEPTED'")
    Optional<FollowsJpa> findAcceptedById(FollowsId id);
}
