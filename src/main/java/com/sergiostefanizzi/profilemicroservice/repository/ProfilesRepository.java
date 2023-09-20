package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfilesRepository extends JpaRepository<ProfileJpa, Long> {
    @Query("SELECT p FROM ProfileJpa p WHERE p.profileName LIKE :profileName% AND p.deletedAt IS NULL")
    List<ProfileJpa> findAllActiveByProfileName(@Param("profileName") String profileName);

    @Query("SELECT p.profileName FROM ProfileJpa p WHERE p.profileName = :profileName AND p.deletedAt IS NULL")
    Optional<String> checkActiveByProfileName(String profileName);

    @Query("SELECT p FROM ProfileJpa p WHERE p.id=:profileId AND p.deletedAt IS NULL")
    Optional<ProfileJpa> findActiveById(Long profileId);

    @Query("SELECT p.id FROM ProfileJpa p WHERE p.id=:profileId AND p.deletedAt IS NULL")
    Optional<Long> checkActiveById(Long profileId);

    @Modifying
    @Query("UPDATE ProfileJpa p SET p.deletedAt = :removalDate WHERE p.id = :profileId")
    void removeProfileByProfileId(Long profileId, LocalDateTime removalDate);
}
