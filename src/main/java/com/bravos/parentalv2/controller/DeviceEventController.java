package com.bravos.parentalv2.controller;

import com.bravos.parentalv2.dto.ApiResponse;
import com.bravos.parentalv2.dto.DeviceEventResponse;
import com.bravos.parentalv2.service.DeviceEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices/{deviceId}/events")
@Tag(name = "Device Events", description = "Device activity history")
public class DeviceEventController {

  private final DeviceEventService deviceEventService;

  public DeviceEventController(DeviceEventService deviceEventService) {
    this.deviceEventService = deviceEventService;
  }

  @GetMapping
  @Operation(summary = "Get device event history")
  public ResponseEntity<ApiResponse<Page<DeviceEventResponse>>> getEvents(
      @PathVariable String deviceId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<DeviceEventResponse> events = deviceEventService.getEventsForDevice(deviceId, page, size);
    return ResponseEntity.ok(ApiResponse.success(events));
  }

}
