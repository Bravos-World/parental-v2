package com.bravos.parentalv2.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "websocket_secret")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketSecret {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "secret_key", nullable = false, unique = true, length = 64)
  private String secretKey;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();

}

