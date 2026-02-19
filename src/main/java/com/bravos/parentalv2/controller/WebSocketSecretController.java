package com.bravos.parentalv2.controller;

import com.bravos.parentalv2.dto.ApiResponse;
import com.bravos.parentalv2.dto.WebSocketSecretResponse;
import com.bravos.parentalv2.model.WebSocketSecret;
import com.bravos.parentalv2.service.WebSocketSecretService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ws-secret")
@Tag(name = "WebSocket Secret", description = "Manage the WebSocket authentication secret key")
public class WebSocketSecretController {

  private final WebSocketSecretService secretService;

  public WebSocketSecretController(WebSocketSecretService secretService) {
    this.secretService = secretService;
  }

  @GetMapping
  @Operation(summary = "View the current WebSocket secret key")
  public ResponseEntity<ApiResponse<WebSocketSecretResponse>> getKey() {
    return secretService.getCurrentSecret()
        .map(secret -> ResponseEntity.ok(ApiResponse.success(toResponse(secret))))
        .orElseGet(() -> ResponseEntity.ok(
            ApiResponse.error("No secret key has been generated yet. Use POST /api/ws-secret/generate to create one.")));
  }

  @PostMapping("/generate")
  @Operation(summary = "Generate a new WebSocket secret key (replaces existing key)")
  public ResponseEntity<ApiResponse<WebSocketSecretResponse>> generateKey() {
    WebSocketSecret secret = secretService.generateKey();
    return ResponseEntity.ok(ApiResponse.success("New secret key generated", toResponse(secret)));
  }

  @PostMapping("/renew")
  @Operation(summary = "Renew (rotate) the WebSocket secret key")
  public ResponseEntity<ApiResponse<WebSocketSecretResponse>> renewKey() {
    WebSocketSecret secret = secretService.renewKey();
    return ResponseEntity.ok(ApiResponse.success("Secret key renewed successfully", toResponse(secret)));
  }

  private WebSocketSecretResponse toResponse(WebSocketSecret secret) {
    return WebSocketSecretResponse.builder()
        .secretKey(secret.getSecretKey())
        .createdAt(secret.getCreatedAt())
        .updatedAt(secret.getUpdatedAt())
        .build();
  }

}




