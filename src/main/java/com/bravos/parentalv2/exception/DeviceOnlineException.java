package com.bravos.parentalv2.exception;

public class DeviceOnlineException extends RuntimeException {

  public DeviceOnlineException(String deviceId) {
    super("Cannot delete online device: " + deviceId);
  }

}

