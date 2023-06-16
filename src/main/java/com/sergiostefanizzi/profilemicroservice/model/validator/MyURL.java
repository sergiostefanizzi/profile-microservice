package com.sergiostefanizzi.profilemicroservice.model.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {MyURLValidator.class})
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyURL {
    String message() default "must be a valid URL";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
