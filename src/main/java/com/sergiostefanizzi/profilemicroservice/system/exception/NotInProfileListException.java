package com.sergiostefanizzi.profilemicroservice.system.exception;

public class NotInProfileListException extends RuntimeException{
    public NotInProfileListException(String message) {
        super(message);
    }

    public NotInProfileListException(Long message) {
        super(String.valueOf(message));
    }
}
