package com.sergiostefanizzi.profilemicroservice.system.exception;

public class IdsMismatchException extends RuntimeException{
    public IdsMismatchException(String message) {
        super(message);
    }

    public IdsMismatchException() {
    }
}
