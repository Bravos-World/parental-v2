package com.bravos.parentalv2.service;

import com.bravos.parentalv2.model.WebSocketSecret;
import com.bravos.parentalv2.repository.WebSocketSecretRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class WebSocketSecretService {

  private final WebSocketSecretRepository repository;
  private final SecureRandom secureRandom = new SecureRandom();

  public WebSocketSecretService(WebSocketSecretRepository repository) {
    this.repository = repository;
  }

  public Optional<WebSocketSecret> getCurrentSecret() {
    return repository.findFirstByOrderByIdDesc();
  }

  @Transactional
  public WebSocketSecret generateKey() {
    repository.deleteAll();
    String key = generateSecureKey();
    WebSocketSecret secret = WebSocketSecret.builder()
        .secretKey(key)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    return repository.save(secret);
  }

  @Transactional
  public WebSocketSecret renewKey() {
    WebSocketSecret existing = repository.findFirstByOrderByIdDesc()
        .orElseGet(() -> WebSocketSecret.builder()
            .createdAt(LocalDateTime.now())
            .build());
    existing.setSecretKey(generateSecureKey());
    existing.setUpdatedAt(LocalDateTime.now());
    return repository.save(existing);
  }

  public boolean validateKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return repository.existsBySecretKey(key);
  }

  private String generateSecureKey() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

}

