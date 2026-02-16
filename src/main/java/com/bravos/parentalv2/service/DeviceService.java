package com.bravos.parentalv2.service;

import com.bravos.parentalv2.dto.DeviceResponse;
import com.bravos.parentalv2.exception.DeviceNotFoundException;
import com.bravos.parentalv2.model.Device;
import com.bravos.parentalv2.model.DeviceStatus;
import com.bravos.parentalv2.model.LockStatus;
import com.bravos.parentalv2.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceService {

  private final DeviceRepository deviceRepository;

  public DeviceService(DeviceRepository deviceRepository) {
    this.deviceRepository = deviceRepository;
  }

  @Transactional
  public Device registerOrUpdate(String deviceId, String deviceName, String ipAddress) {
    Device device = deviceRepository.findByDeviceId(deviceId)
        .orElseGet(() -> Device.builder()
            .deviceId(deviceId)
            .build());

    device.setDeviceName(deviceName);
    device.setIpAddress(ipAddress);
    device.setStatus(DeviceStatus.ONLINE);
    device.setLockStatus(LockStatus.LOCKED);
    device.setLastSeen(LocalDateTime.now());

    return deviceRepository.save(device);
  }

  public List<DeviceResponse> getAllDevices() {
    return deviceRepository.findAll().stream()
        .map(this::toResponse)
        .toList();
  }

  public List<DeviceResponse> getOnlineDevices() {
    return deviceRepository.findAllByStatus(DeviceStatus.ONLINE).stream()
        .map(this::toResponse)
        .toList();
  }

  public DeviceResponse getDevice(String deviceId) {
    Device device = findByDeviceId(deviceId);
    return toResponse(device);
  }

  @Transactional
  public void markOnline(String deviceId) {
    Device device = findByDeviceId(deviceId);
    device.setStatus(DeviceStatus.ONLINE);
    device.setLastSeen(LocalDateTime.now());
    deviceRepository.save(device);
  }

  @Transactional
  public void markOffline(String deviceId) {
    deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
      device.setStatus(DeviceStatus.OFFLINE);
      device.setLastSeen(LocalDateTime.now());
      deviceRepository.save(device);
    });
  }

  @Transactional
  public void updateLockStatus(String deviceId, LockStatus lockStatus) {
    Device device = findByDeviceId(deviceId);
    device.setLockStatus(lockStatus);
    device.setLastSeen(LocalDateTime.now());
    deviceRepository.save(device);
  }

  public Device findByDeviceId(String deviceId) {
    return deviceRepository.findByDeviceId(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
  }

  private DeviceResponse toResponse(Device device) {
    return DeviceResponse.builder()
        .deviceId(device.getDeviceId())
        .deviceName(device.getDeviceName())
        .ipAddress(device.getIpAddress())
        .status(device.getStatus())
        .lockStatus(device.getLockStatus())
        .lastSeen(device.getLastSeen())
        .createdAt(device.getCreatedAt())
        .build();
  }

}
