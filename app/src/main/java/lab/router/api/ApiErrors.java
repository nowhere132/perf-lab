package lab.router.api;

import lab.router.gateway.GatewayException;
import lab.router.service.OrderRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(OrderRejectedException.class)
    public ResponseEntity<Map<String, String>> rejected(OrderRejectedException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<Map<String, String>> gateway(GatewayException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
    }
}
