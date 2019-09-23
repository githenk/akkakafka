package demo.akkakafka.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponseDTO {

  @JsonProperty
  private final String errorMessage;

  public ErrorResponseDTO(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}
