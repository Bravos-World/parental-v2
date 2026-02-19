package com.bravos.parentalv2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketSecretResponse {

  private String secretKey;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}

