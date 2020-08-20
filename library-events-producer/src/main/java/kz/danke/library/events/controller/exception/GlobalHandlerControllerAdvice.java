package kz.danke.library.events.controller.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalHandlerControllerAdvice extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        final String fieldNotValidMessage = "Field is not valid";

        Map<String, String> fieldErrorMap = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> {
                            String defaultMessage = fieldError.getDefaultMessage();

                            if (!StringUtils.isEmpty(defaultMessage)) {
                                return defaultMessage;
                            } else {
                                return fieldNotValidMessage;
                            }
                        }
                ));
        log.error(fieldErrorMap.toString());

        return ResponseEntity
                .status(status)
                .headers(headers)
                .body(fieldErrorMap);
    }
}
