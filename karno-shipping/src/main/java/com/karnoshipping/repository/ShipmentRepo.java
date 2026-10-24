package com.karnoshipping.repository;

import com.karnoshipping.entity.Shipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;
import java.util.List;

@Mapper
public interface ShipmentRepo {
    int insert(Shipment shipment);
    Shipment findByOrderId(@Param("orderId") UUID orderId);
    int markReadyForPickup(@Param("orderId") UUID orderId, @Param("shopId") UUID shopId);
    List<Shipment> findReadyForPickupByShopId(@Param("shopId") UUID shopId);
    List<Shipment> findByIdsForUpdate(@Param("shipmentIds") List<UUID> shipmentIds);
    List<Shipment> findByPickupRequestIdForUpdate(@Param("pickupRequestId") UUID pickupRequestId);
    int assignToPickupRequest(@Param("pickupRequestId") UUID pickupRequestId,
                              @Param("shopId") UUID shopId,
                              @Param("shipmentIds") List<UUID> shipmentIds);
    int releaseFromPickupRequest(@Param("pickupRequestId") UUID pickupRequestId);
    List<Shipment> findByPickupRequestIdsForUpdate(@Param("pickupRequestIds") List<UUID> ids);
    int markPickupPlanned(@Param("pickupRequestIds") List<UUID> ids);
    int revertPickupPlanned(@Param("pickupRequestIds") List<UUID> ids);
    int markPickedUp(@Param("pickupRequestId") UUID pickupRequestId,
                     @Param("shipmentIds") List<UUID> shipmentIds);
    int revertPickedUp(@Param("pickupRequestId") UUID pickupRequestId,
                       @Param("shipmentIds") List<UUID> shipmentIds);
    List<Shipment> findByPickupRequestId(@Param("pickupRequestId") UUID pickupRequestId);
}
