package com.bravos.parentalv2.repository;

import com.bravos.parentalv2.model.Device;
import com.bravos.parentalv2.model.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

  Optional<Device> findByDeviceId(String deviceId);

  List<Device> findAllByStatus(DeviceStatus status);

  boolean existsByDeviceId(String deviceId);

}
