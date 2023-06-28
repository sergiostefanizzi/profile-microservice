package com.sergiostefanizzi.profilemicroservice.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class GeneralExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = ex.getMessage();

        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<Object> handleProfileNotFoundException(PostNotFoundException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Post "+ex.getMessage()+" not found!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }


    @ExceptionHandler(ProfileAlreadyCreatedException.class)
    public ResponseEntity<Object> handleProfileAlreadyCreatedException(ProfileAlreadyCreatedException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Conflict! Profile with name "+ex.getMessage()+" already created!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Object> handleProfileNotFoundException(ProfileNotFoundException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Profile "+ex.getMessage()+" not found!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    // INTERNAL SERVER ERROR
    @ExceptionHandler({RuntimeException.class, Exception.class})
    public ResponseEntity<Object> handleNotManagedException(RuntimeException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Internal Server Error!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    // OVERRIDE METHODS
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ex.getMessage(),ex);
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
        log.error(ex.getMessage(),ex);
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }


    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ex.getMessage(),ex);
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ex.getMessage(),ex);
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
