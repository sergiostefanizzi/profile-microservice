package com.sergiostefanizzi.profilemicroservice.system.exception;

public class AlertNotFoundException extends RuntimeException{

    public AlertNotFoundException(Long alertId) {
        super(alertId.toString());
    }
    public AlertNotFoundException(String message) {
        super(message);
    }
}
