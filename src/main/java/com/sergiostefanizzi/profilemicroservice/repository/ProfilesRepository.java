package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfilesRepository extends JpaRepository<ProfileJpa, Long> {
    @Query("SELECT p FROM ProfileJpa p WHERE p.profileName LIKE :profileName%")
    List<ProfileJpa> findAllByProfileName(@Param("profileName") String profileName);

    Optional<ProfileJpa> findByProfileName(String profileName);

    @Query("SELECT p FROM ProfileJpa p WHERE p.id=:profileId AND p.deletedAt IS NULL")
    Optional<ProfileJpa> findActiveById(Long profileId);
}
