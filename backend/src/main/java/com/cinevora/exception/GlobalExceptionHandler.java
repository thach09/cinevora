package com.cinevora.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.cinevora.common.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> unauthenticated() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid credentials or session"));
    }

    @ExceptionHandler(RateLimitException.class)
    ResponseEntity<ApiResponse<Void>> rateLimit() {
        return ResponseEntity.status(429).header("Retry-After", "900").body(ApiResponse.error("Too many requests"));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> malformed() { return ResponseEntity.badRequest().body(ApiResponse.error("Invalid request")); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> oversizedUpload() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.error("Upload too large"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> unsupportedMediaType() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiResponse.error("Unsupported media type"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> missingStaticResource() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Resource not found"));
    }

    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    ResponseEntity<ApiResponse<Void>> badRequest(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> validation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>(
                        false,
                        "Validation failed",
                        errors,
                        Instant.now()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "Bạn không có quyền thực hiện thao tác này"
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> conflict() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "Dữ liệu đã tồn tại hoặc đang được tham chiếu"
                ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Đã xảy ra lỗi hệ thống"
                ));
    }
}
