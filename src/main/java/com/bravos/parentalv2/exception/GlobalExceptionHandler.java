package com.bravos.parentalv2.exception;

import com.bravos.parentalv2.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(DeviceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceNotFound(DeviceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(DeviceOfflineException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceOffline(DeviceOfflineException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(DeviceOnlineException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceOnline(DeviceOnlineException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error("Invalid username or password"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String errors = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("Internal server error"));
  }

}
