package com.sergiostefanizzi.profilemicroservice.system.exception;

public class EmailNotValidatedException extends RuntimeException{
    public EmailNotValidatedException(String message) {
        super(message);
    }
}
