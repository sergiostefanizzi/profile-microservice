package com.sergiostefanizzi.profilemicroservice.system.exception;

import lombok.RequiredArgsConstructor;


public class CommentNotFoundException extends RuntimeException{
    public CommentNotFoundException(String message) {
        super(message);
    }

    public CommentNotFoundException(Long commentId) {
        super(commentId.toString());
    }
}
