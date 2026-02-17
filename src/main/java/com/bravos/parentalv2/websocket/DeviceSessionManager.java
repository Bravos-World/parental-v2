package com.bravos.parentalv2.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceSessionManager {

  private static final Logger log = LoggerFactory.getLogger(DeviceSessionManager.class);

  private final Map<String, WebSocketSession> deviceSessions = new ConcurrentHashMap<>();
  private final Map<String, String> sessionToDevice = new ConcurrentHashMap<>();

  public void registerSession(String deviceId, WebSocketSession session) {
    WebSocketSession existingSession = deviceSessions.get(deviceId);
    if (existingSession != null && existingSession.isOpen()) {
      try {
        existingSession.close();
      } catch (IOException e) {
        log.warn("Error closing existing session for device {}", deviceId, e);
      }
    }

    deviceSessions.put(deviceId, session);
    sessionToDevice.put(session.getId(), deviceId);
    log.info("Device registered: {} (session: {})", deviceId, session.getId());
  }

  public void removeSession(WebSocketSession session) {
    String deviceId = sessionToDevice.remove(session.getId());
    if (deviceId != null) {
      deviceSessions.remove(deviceId);
      log.info("Device unregistered: {} (session: {})", deviceId, session.getId());
    }
  }

  public String getDeviceId(WebSocketSession session) {
    return sessionToDevice.get(session.getId());
  }

  public WebSocketSession getSession(String deviceId) {
    return deviceSessions.get(deviceId);
  }

  public boolean isOnline(String deviceId) {
    WebSocketSession session = deviceSessions.get(deviceId);
    return session != null && session.isOpen();
  }

  public Set<String> getOnlineDeviceIds() {
    return Collections.unmodifiableSet(deviceSessions.keySet());
  }

  public void sendToDevice(String deviceId, String message) throws IOException {
    WebSocketSession session = deviceSessions.get(deviceId);
    if (session != null && session.isOpen()) {
      session.sendMessage(new TextMessage(message));
    } else {
      throw new IOException("Device " + deviceId + " is not connected");
    }
  }

  public void sendToAll(String message) {
    deviceSessions.forEach((deviceId, session) -> {
      if (session.isOpen()) {
        try {
          session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
          log.error("Error sending message to device {}", deviceId, e);
        }
      }
    });
  }

}
