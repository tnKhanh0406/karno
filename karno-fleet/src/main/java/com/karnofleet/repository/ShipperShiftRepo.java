package com.karnofleet.repository;

import com.karnofleet.entity.ShipperShift;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper
public interface ShipperShiftRepo {
    ShipperShift findActiveShift(
            @Param("shipperUserId") UUID shipperUserId,
            @Param("now") LocalDateTime now
    );
}
