package com.karnofleet.repository;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.enums.PickupRequestStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface PickupRequestRepo {
    List<PickupRequest> findPlanningCandidates(
            @Param("hubId") UUID hubId,
            @Param("status") PickupRequestStatus status
    );
}
