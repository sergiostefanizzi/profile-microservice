package com.sergiostefanizzi.profilemicroservice.system.exception;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

public class ProfileAlreadyCreatedException extends RuntimeException {
    public ProfileAlreadyCreatedException(String message) {
        super(message);
    }
}
