package com.sergiostefanizzi.profilemicroservice.system.exception;


public class ProfileAlreadyCreatedException extends RuntimeException {
    public ProfileAlreadyCreatedException(String message) {
        super(message);
    }
}
