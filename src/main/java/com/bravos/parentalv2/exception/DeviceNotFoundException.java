package com.bravos.parentalv2.exception;

public class DeviceNotFoundException extends RuntimeException {

  public DeviceNotFoundException(String deviceId) {
    super("Device not found: " + deviceId);
  }

}
