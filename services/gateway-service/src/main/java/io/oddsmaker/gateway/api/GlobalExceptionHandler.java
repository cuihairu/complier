package io.oddsmaker.gateway.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> handleRSE(ResponseStatusException ex, ServerWebExchange exchange) {
        String rid = (String) exchange.getAttributes().getOrDefault("x-request-id", "");
        HttpStatusCode statusCode = ex.getStatusCode();
        return ResponseEntity.status(statusCode)
                .body(Map.of(
                        "code", toCode(statusCode),
                        "message", ex.getReason() == null ? statusCode.toString() : ex.getReason(),
                        "request_id", rid
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleAny(Exception ex, ServerWebExchange exchange) {
        String rid = (String) exchange.getAttributes().getOrDefault("x-request-id", "");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "internal_error",
                        "message", "internal_error",
                        "request_id", rid
                ));
    }

    private String toCode(HttpStatusCode status) {
        int value = status.value();
        if (value == 413) return "payload_too_large";
        if (value == 429) return "too_many_requests";
        if (value == 401) return "unauthorized";
        if (value == 403) return "forbidden";
        if (value == 400) return "bad_request";
        return status.toString().toLowerCase();
    }
}
