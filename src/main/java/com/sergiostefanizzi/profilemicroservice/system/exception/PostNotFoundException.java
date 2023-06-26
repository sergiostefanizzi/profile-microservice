package com.sergiostefanizzi.profilemicroservice.system.exception;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(Long postId) {
        super(postId.toString());
    }

    public PostNotFoundException(String message) {
        super(message);
    }
}
