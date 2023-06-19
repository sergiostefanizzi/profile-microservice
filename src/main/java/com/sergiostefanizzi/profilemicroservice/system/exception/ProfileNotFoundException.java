package com.sergiostefanizzi.profilemicroservice.system.exception;

public class ProfileNotFoundException extends RuntimeException{
    public ProfileNotFoundException(String message) {
        super(message);
    }

    public ProfileNotFoundException(Long profileId) {
        super(profileId.toString());
    }
}
