package com.sergiostefanizzi.profilemicroservice.model.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.UrlValidator;

public class MyURLValidator implements ConstraintValidator<MyURL, String> {
    @Override
    public void initialize(MyURL constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        if (url == null){
            return true;
        }
        UrlValidator urlValidator = new UrlValidator();
        return urlValidator.isValid(url);
    }
}
