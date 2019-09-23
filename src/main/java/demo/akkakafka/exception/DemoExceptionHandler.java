package demo.akkakafka.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.concurrent.ExecutionException;

@ControllerAdvice
public class DemoExceptionHandler {

  @ExceptionHandler(value = {InterruptedException.class, ExecutionException.class})
  public ResponseEntity<ErrorResponseDTO> handleException(Exception ex) {

    ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(ex.getMessage());

    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status).body(errorResponseDTO);

  }
}
