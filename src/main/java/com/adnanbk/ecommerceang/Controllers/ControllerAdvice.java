package com.adnanbk.ecommerceang.Controllers;

import com.adnanbk.ecommerceang.dto.ApiError;
import com.adnanbk.ecommerceang.dto.ResponseError;
import com.adnanbk.ecommerceang.models.Product;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ControllerAdvice {


    @ExceptionHandler({ PersistenceException.class,ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleConstraintViolation(
            RuntimeException ex) {
        System.out.println("******persistence exceptio******");
        if(ex  instanceof ConstraintViolationException )
        {
            ConstraintViolationException cause = (ConstraintViolationException) ex;
            return ResponseEntity.badRequest().body(generateErrors(cause));
        }
        if(NestedExceptionUtils.getRootCause(ex)  instanceof ConstraintViolationException)
        {
            ConstraintViolationException cause = (ConstraintViolationException) NestedExceptionUtils.getRootCause(ex);
            return ResponseEntity.badRequest().body(generateErrors(cause));
        }
        
         if(NestedExceptionUtils.getRootCause(ex) instanceof SQLIntegrityConstraintViolationException)
        {
            return ResponseEntity.badRequest().body("You are trying to insert an existing value  , try another one");
        }
        if(ex  instanceof DataIntegrityViolationException) {
            return ResponseEntity.badRequest().body("You are trying to insert an existing value  , try another one");
        }
        return ResponseEntity.badRequest().body("An error has been thrown during database modification ");
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        Set<Object> errors = new HashSet<>();

        ex.getBindingResult().getFieldErrors().forEach(
                        er->
                        errors.add(new ResponseError(er.getField(),er.getDefaultMessage()))
        );

        ex.getBindingResult().getGlobalErrors()
                .forEach(x -> {
                    if(x.getDefaultMessage()!=null)
                        errors.add(new ResponseError(Objects.requireNonNull(x.getCode()),x.getDefaultMessage()));
                          else
                           errors.add(x.getCode());
                });
        // body.put("errors", errors);
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Try to fix these errors", errors);
        return   ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException ex) {
        Set<Object> errors = Set.of(ex.getMessage());
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Try to fix these errors", errors);
        return   ResponseEntity.badRequest().body(apiError);
    }
    private Set<String> generateErrors(ConstraintViolationException cause) {
        Set<String> errors = new HashSet<>();
        for (ConstraintViolation<?> violation : cause.getConstraintViolations()) {
            String message = violation.getMessage();
            if(violation.getPropertyPath()!=null)
                message = new ResponseError(violation.getPropertyPath().toString(),violation.getMessage()).toString();
            errors.add(message);
        }
        return errors;
    }

}
