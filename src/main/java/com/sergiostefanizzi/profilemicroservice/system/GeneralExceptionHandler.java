package com.sergiostefanizzi.profilemicroservice.system;

import com.sergiostefanizzi.profilemicroservice.system.exception.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

    @ExceptionHandler(AlertStatusNotValidException.class)
    public ResponseEntity<Object> handleAlertStatusNotValidException(AlertStatusNotValidException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Alert status not valid!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(FollowItselfException.class)
    public ResponseEntity<Object> handleFollowItselfException(FollowItselfException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
    @ExceptionHandler(AlertTypeErrorException.class)
    public ResponseEntity<Object> handleAlertTypeNotSpecifiedException(AlertTypeErrorException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(FollowNotFoundException.class)
    public ResponseEntity<Object> handleFollowNotFoundException(FollowNotFoundException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Follows not found!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UnfollowOnCreationException.class)
    public ResponseEntity<Object> handleUnfollowOnCreationException(UnfollowOnCreationException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Unfollows on creation is not possible!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<Object> handleCommentNotFoundException(CommentNotFoundException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Comment "+ex.getMessage()+" not found!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(CommentOnStoryException.class)
    public ResponseEntity<Object> handleCommentOnStoryException(CommentOnStoryException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Cannot comment on a story!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

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

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<Object> handleAlertNotFoundException(AlertNotFoundException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Alert "+ex.getMessage()+" not found!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(NotInProfileListException.class)
    public ResponseEntity<Object> handleNotInProfileListException(NotInProfileListException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Profile "+ex.getMessage()+" is not inside the profile list!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Object> handleProfileNotFoundException(ProfileNotFoundException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "Profile "+ex.getMessage()+" not found!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Object> handleNumberFormatException(NumberFormatException ex, WebRequest request){
        log.error(ex.getMessage(),ex);
        String error = "ID is not valid!";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
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
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ex.getMessage(),ex);
        String error = ex.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
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
        String error = "Type mismatch";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }


    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ex.getMessage(),ex);
        String error = "Message is not readable";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ex.getMessage(),ex);
        String error = "Message is not writable";
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
