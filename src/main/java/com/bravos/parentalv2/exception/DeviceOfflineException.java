package com.bravos.parentalv2.exception;

public class DeviceOfflineException extends RuntimeException {

  public DeviceOfflineException(String deviceId) {
    super("Device is offline: " + deviceId);
  }

}
