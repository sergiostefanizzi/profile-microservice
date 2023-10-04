package com.sergiostefanizzi.profilemicroservice.system.exception;

public class AlertStatusNotValidException extends RuntimeException{
    public AlertStatusNotValidException(String message) {
        super(message);
    }

    public AlertStatusNotValidException(){}
}
