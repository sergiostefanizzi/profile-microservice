package com.sergiostefanizzi.profilemicroservice.system.exception;

public class AlertTypeNotSpecifiedException extends RuntimeException{
    public AlertTypeNotSpecifiedException(String message) {
        super(message);
    }
}
