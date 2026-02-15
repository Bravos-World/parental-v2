package com.bravos.parentalv2.repository;

import com.bravos.parentalv2.model.Device;
import com.bravos.parentalv2.model.DeviceEvent;
import com.bravos.parentalv2.model.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {

  Page<DeviceEvent> findByDeviceOrderByTimestampDesc(Device device, Pageable pageable);

  List<DeviceEvent> findByDeviceAndEventTypeOrderByTimestampDesc(Device device, EventType eventType);

}
