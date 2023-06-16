package com.sergiostefanizzi.profilemicroservice.system;

import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ProfileExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProfileAlreadyCreatedException.class)
    public ResponseEntity<Object> handleProfileAlreadyCreatedException(ProfileAlreadyCreatedException ex, WebRequest request){
        String error = "Conflict! Profile with name "+ex.getMessage()+" already created!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    // INTERNAL SERVER ERROR
    @ExceptionHandler({RuntimeException.class, Exception.class})
    public ResponseEntity<Object> handleNotManagedException(RuntimeException ex, WebRequest request){
        String error = "Internal Server Error!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    // OVERRIDE METHODS
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField()+" "+error.getDefaultMessage())
                .collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("error", errors);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }


    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }


    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}