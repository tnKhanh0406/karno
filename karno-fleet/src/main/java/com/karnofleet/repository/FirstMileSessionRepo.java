package com.karnofleet.repository;

import com.karnofleet.entity.PickupSession;
import com.karnofleet.entity.ServiceZone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.UUID;

@Mapper
public interface FirstMileSessionRepo {
    ServiceZone findActiveZoneByShipper(@Param("shipperId") UUID shipperId);
    PickupSession findActiveByShipper(@Param("shipperId") UUID shipperId);
    int insert(PickupSession session);
}
