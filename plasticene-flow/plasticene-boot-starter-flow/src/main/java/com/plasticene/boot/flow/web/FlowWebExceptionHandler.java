package com.plasticene.boot.flow.web;

import com.plasticene.boot.flow.core.FlowException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将审批流 HTTP 接口异常转换为稳定的错误响应。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@RestControllerAdvice(assignableTypes = FlowController.class)
public class FlowWebExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("FLOW_REQUEST_INVALID", exception.getMessage()));
    }

    @ExceptionHandler(FlowException.class)
    public ResponseEntity<ErrorResponse> handleFlowException(FlowException exception) {
        HttpStatus status = statusOf(exception.getCode());
        return ResponseEntity.status(status).body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    private HttpStatus statusOf(String code) {
        if (code.endsWith("_NOT_FOUND")) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.endsWith("_FORBIDDEN")) {
            return HttpStatus.FORBIDDEN;
        }
        if (code.contains("ALREADY") || code.endsWith("_NOT_RUNNING")
                || code.endsWith("_NOT_ALLOWED") || code.endsWith("_IN_PROGRESS")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public record ErrorResponse(String code, String message) {
    }
}
