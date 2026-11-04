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
    List<Vehicle> findAvailableForShipper(@Param("shipperId") UUID shipperId,
                                          @Param("hubId") UUID hubId);
    int acquire(@Param("vehicleId") UUID vehicleId, @Param("shipperId") UUID shipperId,
                @Param("hubId") UUID hubId);
    int release(@Param("vehicleId") UUID vehicleId);
    Optional<Vehicle> findUsableBySession(@Param("vehicleId") UUID vehicleId,
                                          @Param("shipperId") UUID shipperId,
                                          @Param("hubId") UUID hubId);
}
