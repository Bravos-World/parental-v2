package com.bravos.parentalv2.controller;

import com.bravos.parentalv2.dto.ApiResponse;
import com.bravos.parentalv2.dto.CommandRequest;
import com.bravos.parentalv2.dto.DeviceResponse;
import com.bravos.parentalv2.dto.MessageRequest;
import com.bravos.parentalv2.exception.DeviceOfflineException;
import com.bravos.parentalv2.model.CommandType;
import com.bravos.parentalv2.model.EventType;
import com.bravos.parentalv2.model.LockStatus;
import com.bravos.parentalv2.service.DeviceEventService;
import com.bravos.parentalv2.service.DeviceService;
import com.bravos.parentalv2.websocket.DeviceSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Device Management", description = "Device listing, commands, and messaging")
public class DeviceController {

  private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

  private final DeviceService deviceService;
  private final DeviceEventService deviceEventService;
  private final DeviceSessionManager sessionManager;
  private final ObjectMapper objectMapper;

  public DeviceController(DeviceService deviceService,
                          DeviceEventService deviceEventService,
                          DeviceSessionManager sessionManager,
                          ObjectMapper objectMapper) {
    this.deviceService = deviceService;
    this.deviceEventService = deviceEventService;
    this.sessionManager = sessionManager;
    this.objectMapper = objectMapper;
  }

  @GetMapping
  @Operation(summary = "List all devices")
  public ResponseEntity<ApiResponse<List<DeviceResponse>>> getAllDevices() {
    return ResponseEntity.ok(ApiResponse.success(deviceService.getAllDevices()));
  }

  @GetMapping("/online")
  @Operation(summary = "List online devices")
  public ResponseEntity<ApiResponse<List<DeviceResponse>>> getOnlineDevices() {
    return ResponseEntity.ok(ApiResponse.success(deviceService.getOnlineDevices()));
  }

  @GetMapping("/{deviceId}")
  @Operation(summary = "Get device details")
  public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(@PathVariable String deviceId) {
    return ResponseEntity.ok(ApiResponse.success(deviceService.getDevice(deviceId)));
  }

  @PostMapping("/{deviceId}/command")
  @Operation(summary = "Send command to a specific device (lock, unlock, shutdown, restart)")
  public ResponseEntity<ApiResponse<Void>> sendCommand(@PathVariable String deviceId,
                                                       @Valid @RequestBody CommandRequest request) {
    if (!sessionManager.isOnline(deviceId)) {
      throw new DeviceOfflineException(deviceId);
    }

    try {
      String commandJson = objectMapper.writeValueAsString(Map.of(
          "type", "command",
          "command", request.getCommandType().name(),
          "delaySeconds", request.getDelaySeconds())
      );
      sessionManager.sendToDevice(deviceId, commandJson);

      // Log the command event
      EventType eventType = mapCommandToEvent(request.getCommandType());
      if (eventType != null) {
        String desc = String.format("%s with %ds delay", request.getCommandType(), request.getDelaySeconds());
        deviceEventService.logEvent(deviceId, eventType, desc);
      }

      // Update lock status for lock/unlock commands
      if (request.getCommandType() == CommandType.LOCK) {
        deviceService.updateLockStatus(deviceId, LockStatus.LOCKED);
      } else if (request.getCommandType() == CommandType.UNLOCK) {
        deviceService.updateLockStatus(deviceId, LockStatus.UNLOCKED);
      }

      return ResponseEntity.ok(ApiResponse.success("Command sent successfully"));
    } catch (IOException e) {
      throw new DeviceOfflineException(deviceId);
    }
  }

  @PostMapping("/{deviceId}/message")
  @Operation(summary = "Send a notification message to a specific device")
  public ResponseEntity<ApiResponse<Void>> sendMessage(@PathVariable String deviceId,
                                                       @Valid @RequestBody MessageRequest request) {
    if (!sessionManager.isOnline(deviceId)) {
      throw new DeviceOfflineException(deviceId);
    }

    try {
      String messageJson = objectMapper.writeValueAsString(Map.of(
          "type", "message",
          "content", request.getMessage()));
      sessionManager.sendToDevice(deviceId, messageJson);
      return ResponseEntity.ok(ApiResponse.success("Message sent successfully"));
    } catch (IOException e) {
      throw new DeviceOfflineException(deviceId);
    }
  }

  @PostMapping("/command")
  @Operation(summary = "Send command to all online devices")
  public ResponseEntity<ApiResponse<Void>> sendCommandToAll(@Valid @RequestBody CommandRequest request) {
    try {
      String commandJson = objectMapper.writeValueAsString(Map.of(
          "type", "command",
          "command", request.getCommandType().name(),
          "delaySeconds", request.getDelaySeconds()));
      sessionManager.sendToAll(commandJson);

      // Log event for all online devices
      EventType eventType = mapCommandToEvent(request.getCommandType());
      for (String deviceId : sessionManager.getOnlineDeviceIds()) {
        if (eventType != null) {
          String desc = String.format("Broadcast %s with %ds delay", request.getCommandType(),
              request.getDelaySeconds());
          deviceEventService.logEvent(deviceId, eventType, desc);
        }
        if (request.getCommandType() == CommandType.LOCK) {
          deviceService.updateLockStatus(deviceId, LockStatus.LOCKED);
        } else if (request.getCommandType() == CommandType.UNLOCK) {
          deviceService.updateLockStatus(deviceId, LockStatus.UNLOCKED);
        }
      }

      return ResponseEntity.ok(ApiResponse.success("Command sent to all devices"));
    } catch (Exception e) {
      log.error("Error sending broadcast command", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Error sending command to devices"));
    }
  }

  @PostMapping("/message")
  @Operation(summary = "Send a notification message to all online devices")
  public ResponseEntity<ApiResponse<Void>> sendMessageToAll(@Valid @RequestBody MessageRequest request) {
    try {
      String messageJson = objectMapper.writeValueAsString(Map.of(
          "type", "message",
          "content", request.getMessage()));
      sessionManager.sendToAll(messageJson);
      return ResponseEntity.ok(ApiResponse.success("Message sent to all devices"));
    } catch (Exception e) {
      log.error("Error sending broadcast message", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("Error sending message to devices"));
    }
  }

  private EventType mapCommandToEvent(CommandType commandType) {
    return switch (commandType) {
      case LOCK -> EventType.LOCK;
      case UNLOCK -> EventType.UNLOCK;
      case SHUTDOWN -> EventType.SHUTDOWN;
      case RESTART -> EventType.RESTART;
      case MESSAGE -> null;
    };
  }

}
