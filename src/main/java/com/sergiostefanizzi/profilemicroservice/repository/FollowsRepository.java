package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowsRepository extends JpaRepository<FollowsJpa, FollowsId> {
    String accepted = "ACCEPTED";
    String pending = "PENDING";

    @Query("SELECT f.follower FROM FollowsJpa f WHERE f.followed = :profileJpa AND f.followedAt IS NOT NULL AND f.unfollowedAt IS NULL AND f.requestStatus = '"+accepted+"' AND f.follower.deletedAt IS NULL")
    List<ProfileJpa> findActiveFollowers(ProfileJpa profileJpa);

    @Query("SELECT f.followed FROM FollowsJpa f WHERE f.follower = :profileJpa AND f.followedAt IS NOT NULL AND f.unfollowedAt IS NULL AND f.requestStatus = '"+accepted+"' AND f.followed.deletedAt IS NULL")
    List<ProfileJpa> findActiveFollowings(ProfileJpa profileJpa);

    @Query("SELECT f FROM FollowsJpa f WHERE f.id = :followsId AND ((f.requestStatus = '"+accepted+"') OR (f.requestStatus = '"+pending+"'))")
    Optional<FollowsJpa> findActiveById(FollowsId followsId);

}
