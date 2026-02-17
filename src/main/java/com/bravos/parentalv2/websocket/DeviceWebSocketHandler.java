package com.bravos.parentalv2.websocket;

import com.bravos.parentalv2.model.CommandType;
import com.bravos.parentalv2.model.Device;
import com.bravos.parentalv2.model.EventType;
import com.bravos.parentalv2.model.LockStatus;
import com.bravos.parentalv2.service.DeviceEventService;
import com.bravos.parentalv2.service.DeviceService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.util.Map;

@Component
public class DeviceWebSocketHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(DeviceWebSocketHandler.class);

  private final DeviceSessionManager sessionManager;
  private final DeviceService deviceService;
  private final DeviceEventService deviceEventService;
  private final ObjectMapper objectMapper;

  private final String lockNowMessage;
  private final String okMessage;

  public DeviceWebSocketHandler(DeviceSessionManager sessionManager,
                                DeviceService deviceService,
                                DeviceEventService deviceEventService,
                                ObjectMapper objectMapper) {
    this.sessionManager = sessionManager;
    this.deviceService = deviceService;
    this.deviceEventService = deviceEventService;
    this.objectMapper = objectMapper;

    this.lockNowMessage = objectMapper.writeValueAsString(Map.of
        (
        "type", "command",
        "command", CommandType.LOCK.name(),
        "delaySeconds", 0
        )
    );
    this.okMessage = objectMapper.writeValueAsString(Map.of("type", "registered", "status", "ok"));
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    log.info("WebSocket connection established: {}", session.getId());
  }

  @Override
  protected void handleTextMessage(@NonNull WebSocketSession session,
                                   @NonNull TextMessage message) {
    try {
      JsonNode json = objectMapper.readTree(message.getPayload());
      String type = json.path("type").asString();
      switch (type) {
        case "register" -> handleRegister(session, json);
        case "status" -> handleStatusUpdate(session, json);
        case "event" -> handleEvent(session, json);
        default -> log.warn("Unknown message type: {} from session {}", type, session.getId());
      }
    } catch (Exception e) {
      log.error("Error handling message from session {}: {}", session.getId(), e.getMessage(), e);
    }
  }

  private void handleRegister(WebSocketSession session, JsonNode json) {
    String deviceId = json.path("deviceId").asString();
    String deviceName = json.path("deviceName").asString();
    InetAddress inetAddress = session.getRemoteAddress() != null ? session.getRemoteAddress().getAddress() : null;
    String ipAddress = inetAddress != null ? inetAddress.getHostAddress() : json.path("ipAddress").asString("unknown");
    if (deviceId.isBlank()) {
      log.warn("Register message missing deviceId from session {}", session.getId());
      return;
    }
    Device device = deviceService.registerOrUpdate(deviceId, deviceName, ipAddress);
    sessionManager.registerSession(device.getDeviceId(), session);
    deviceEventService.logEvent(device.getDeviceId(), EventType.CONNECT);
    log.info("Device registered: {} ({}) from {}", device.getDeviceName(), device.getDeviceId(), ipAddress);
    try {
      session.sendMessage(new TextMessage(okMessage));
      session.sendMessage(new TextMessage(lockNowMessage));
    } catch (Exception e) {
      log.error("Error sending registration ack to device {}", deviceId, e);
    }
  }

  private void handleStatusUpdate(WebSocketSession session, JsonNode json) {
    String deviceId = sessionManager.getDeviceId(session);
    if (deviceId == null) {
      log.warn("Status update from unregistered session {}", session.getId());
      return;
    }

    String lockStatus = json.path("lockStatus").asString();
    if (!lockStatus.isBlank()) {
      try {
        LockStatus status = LockStatus.valueOf(lockStatus.toUpperCase());
        deviceService.updateLockStatus(deviceId, status);
        EventType eventType = status == LockStatus.LOCKED ? EventType.LOCK : EventType.UNLOCK;
        deviceEventService.logEvent(deviceId, eventType);
      } catch (IllegalArgumentException e) {
        log.warn("Invalid lock status: {} from device {}", lockStatus, deviceId);
      }
    }
  }

  private void handleEvent(WebSocketSession session, JsonNode json) {
    String deviceId = sessionManager.getDeviceId(session);
    if (deviceId == null) {
      log.warn("Event from unregistered session {}", session.getId());
      return;
    }

    String eventTypeStr = json.path("eventType").asString();
    String description = json.path("description").asString(null);
    try {
      EventType eventType = EventType.valueOf(eventTypeStr.toUpperCase());
      deviceEventService.logEvent(deviceId, eventType, description);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid event type: {} from device {}", eventTypeStr, deviceId);
    }
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session,
                                    @NonNull CloseStatus status) {
    String deviceId = sessionManager.getDeviceId(session);
    if (deviceId != null) {
      deviceService.markOffline(deviceId);
      deviceEventService.logEvent(deviceId, EventType.DISCONNECT);
      sessionManager.removeSession(session);
      log.info("Device disconnected: {} (reason: {})", deviceId, status);
    } else {
      log.info("Unregistered session disconnected: {}", session.getId());
    }
  }

  @Override
  public void handleTransportError(@NonNull WebSocketSession session, Throwable exception) {
    String deviceId = sessionManager.getDeviceId(session);
    log.error("Transport error for device {}: {}", deviceId != null ? deviceId : session.getId(), exception.getMessage());
  }

}
