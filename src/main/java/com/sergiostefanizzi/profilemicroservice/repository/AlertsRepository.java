package com.sergiostefanizzi.profilemicroservice.repository;

import com.sergiostefanizzi.profilemicroservice.model.AlertJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertsRepository extends JpaRepository<AlertJpa, Long> {
    @Query("SELECT a.id FROM AlertJpa a WHERE a.id = :alertId")
    Optional<Long> checkAlertById(Long alertId);
}
