package com.bravos.parentalv2.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_id", unique = true, nullable = false)
  private String deviceId;

  @Column(name = "device_name")
  private String deviceName;

  @Column(name = "ip_address")
  private String ipAddress;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private DeviceStatus status = DeviceStatus.OFFLINE;

  @Enumerated(EnumType.STRING)
  @Column(name = "lock_status", nullable = false)
  @Builder.Default
  private LockStatus lockStatus = LockStatus.UNLOCKED;

  @Column(name = "last_seen")
  private LocalDateTime lastSeen;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

}
