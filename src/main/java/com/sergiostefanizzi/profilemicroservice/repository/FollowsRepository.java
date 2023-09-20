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
    @Query("SELECT f FROM FollowsJpa f WHERE f.id = :id AND f.followedAt IS NOT NULL")
    Optional<FollowsJpa> findAcceptedById(FollowsId id);

    @Query("SELECT f.follower FROM FollowsJpa f WHERE f.followed = :profileJpa AND f.followedAt IS NOT NULL AND f.unfollowedAt IS NULL")
    List<ProfileJpa> findActiveFollowers(ProfileJpa profileJpa);

    @Query("SELECT f.followed FROM FollowsJpa f WHERE f.follower = :profileJpa AND f.followedAt IS NOT NULL AND f.unfollowedAt IS NULL")
    List<ProfileJpa> findActiveFollowings(ProfileJpa profileJpa);

    @Modifying
    @Query("UPDATE FollowsJpa f SET f.unfollowedAt = :removalDate WHERE (f.follower.id = :profileId OR f.followed.id = :profileId)")
    void removeFollowByProfileId(Long profileId, LocalDateTime removalDate);

}
