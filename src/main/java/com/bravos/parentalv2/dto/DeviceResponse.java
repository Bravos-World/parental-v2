package com.bravos.parentalv2.dto;

import com.bravos.parentalv2.model.DeviceStatus;
import com.bravos.parentalv2.model.LockStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeviceResponse {

  private String deviceId;
  private String deviceName;
  private String ipAddress;
  private DeviceStatus status;
  private LockStatus lockStatus;
  private LocalDateTime lastSeen;
  private LocalDateTime createdAt;

}
