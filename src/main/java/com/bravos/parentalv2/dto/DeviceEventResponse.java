package com.bravos.parentalv2.dto;

import com.bravos.parentalv2.model.EventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeviceEventResponse {

  private Long id;
  private String deviceId;
  private String deviceName;
  private EventType eventType;
  private String description;
  private LocalDateTime timestamp;

}
