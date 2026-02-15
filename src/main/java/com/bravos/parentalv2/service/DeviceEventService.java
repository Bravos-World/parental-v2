package com.bravos.parentalv2.service;

import com.bravos.parentalv2.dto.DeviceEventResponse;
import com.bravos.parentalv2.model.Device;
import com.bravos.parentalv2.model.DeviceEvent;
import com.bravos.parentalv2.model.EventType;
import com.bravos.parentalv2.repository.DeviceEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceEventService {

  private final DeviceEventRepository deviceEventRepository;
  private final DeviceService deviceService;

  public DeviceEventService(DeviceEventRepository deviceEventRepository, DeviceService deviceService) {
    this.deviceEventRepository = deviceEventRepository;
    this.deviceService = deviceService;
  }

  @Transactional
  public void logEvent(String deviceId, EventType eventType) {
    logEvent(deviceId, eventType, null);
  }

  @Transactional
  public void logEvent(String deviceId, EventType eventType, String description) {
    Device device = deviceService.findByDeviceId(deviceId);
    DeviceEvent event = DeviceEvent.builder()
        .device(device)
        .eventType(eventType)
        .description(description)
        .build();
    deviceEventRepository.save(event);
  }

  public Page<DeviceEventResponse> getEventsForDevice(String deviceId, int page, int size) {
    Device device = deviceService.findByDeviceId(deviceId);
    Pageable pageable = PageRequest.of(page, size);
    return deviceEventRepository.findByDeviceOrderByTimestampDesc(device, pageable)
        .map(this::toResponse);
  }

  private DeviceEventResponse toResponse(DeviceEvent event) {
    return DeviceEventResponse.builder()
        .id(event.getId())
        .deviceId(event.getDevice().getDeviceId())
        .deviceName(event.getDevice().getDeviceName())
        .eventType(event.getEventType())
        .description(event.getDescription())
        .timestamp(event.getTimestamp())
        .build();
  }

}
