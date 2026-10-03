package com.example.shop.security;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> handle(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        return ResponseEntity.status(status).body(Map.of("error", Map.of("code", Integer.toString(status), "message", exception.getReason() == null ? "请求失败" : exception.getReason(), "request_id", UUID.randomUUID().toString())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", Map.of("code", "400", "message", exception.getMessage() == null ? "请求参数错误" : exception.getMessage(), "request_id", UUID.randomUUID().toString())));
    }
}
