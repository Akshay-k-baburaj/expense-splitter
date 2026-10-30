package com.akshay.expense_splitter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.akshay.expense_splitter.exception.ExpenseNotFoundException;
import com.akshay.expense_splitter.exception.FinancialOperationException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.FORBIDDEN.value(), "Forbidden", "You are not allowed to perform this action.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> accountDisabled(AccountDisabledException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(EmailVerificationRequiredException.class)
    public ResponseEntity<ErrorResponse> emailVerificationRequired(EmailVerificationRequiredException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(LegacyAccountLinkRequiredException.class)
    public ResponseEntity<ErrorResponse> legacyAccountLinkRequired(LegacyAccountLinkRequiredException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception){
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName,errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> accountNotFoundException(AccountNotFoundException ex){
            ErrorResponse error = new ErrorResponse(
                    OffsetDateTime.now(),
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> groupNotFoundException(GroupNotFoundException ex){
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DuplicateMembershipException.class)
    public ResponseEntity<ErrorResponse> duplicateMembershipFound(DuplicateMembershipException ex){
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Joining already joined group",
                ex.getMessage()

        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidGroupInviteCodeException.class)
    public ResponseEntity<ErrorResponse> invalidGroupInviteCode(InvalidGroupInviteCodeException ex) {
        ErrorResponse error = new ErrorResponse(OffsetDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Invalid group code", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(GroupJoinRateLimitException.class)
    public ResponseEntity<ErrorResponse> groupJoinRateLimit(GroupJoinRateLimitException ex) {
        ErrorResponse error = new ErrorResponse(OffsetDateTime.now(), HttpStatus.TOO_MANY_REQUESTS.value(), "Too many attempts", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public ResponseEntity<ErrorResponse> expenseNotFound(ExpenseNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(OffsetDateTime.now(), HttpStatus.NOT_FOUND.value(), "Expense not found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(FinancialOperationException.class)
    public ResponseEntity<ErrorResponse> financialOperation(FinancialOperationException ex) {
        ErrorResponse error = new ErrorResponse(OffsetDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Invalid financial operation", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> duplicateUserDetails(DuplicateUserException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> memberNotFoundException(MemberNotFoundException ex){
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
