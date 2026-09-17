package com.cinevora.exception;

import com.cinevora.common.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException ex) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage())); }
    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    ResponseEntity<ApiResponse<Void>> badRequest(RuntimeException ex) { return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) errors.putIfAbsent(e.getField(), e.getDefaultMessage());
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation failed", errors, java.time.Instant.now()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden() { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> conflict() { return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Dữ liệu đã tồn tại hoặc đang được tham chiếu")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Đã xảy ra lỗi hệ thống")); }
}
