package com.sergiostefanizzi.profilemicroservice.system.exception;

public class AlertTypeErrorException extends RuntimeException{
    public AlertTypeErrorException(String message) {
        super(message);
    }
}
