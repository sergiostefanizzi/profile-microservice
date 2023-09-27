package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.AlertsApi;
import com.sergiostefanizzi.profilemicroservice.model.Alert;
import com.sergiostefanizzi.profilemicroservice.service.AlertsService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AlertsController implements AlertsApi {
    private final AlertsService alertsService;

    @Override
    public ResponseEntity<Alert> createAlert(Boolean isPost, Alert alert) {
        Alert savedAlert = this.alertsService.createAlert(isPost, alert);
        return new ResponseEntity<>(savedAlert, HttpStatus.CREATED);
    }
}
