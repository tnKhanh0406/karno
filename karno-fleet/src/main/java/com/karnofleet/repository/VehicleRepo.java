package com.karnofleet.repository;

import com.karnofleet.entity.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VehicleRepo {
    int insert(Vehicle vehicle);

    Optional<Vehicle> findById(@Param("id") UUID id);

    Optional<Vehicle> findByVehicleCode(@Param("vehicleCode") String vehicleCode);

    List<Vehicle> findAll();

    List<Vehicle> findByStatus(@Param("status") String status);
}
