package com.sergiostefanizzi.profilemicroservice.system.exception;

public class PostAccessForbiddenException extends RuntimeException{
    public PostAccessForbiddenException(Long message) {
        super(String.valueOf(message));
    }
}
