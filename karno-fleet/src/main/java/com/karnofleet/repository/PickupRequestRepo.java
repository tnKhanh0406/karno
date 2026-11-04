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
    int insert(PickupRequest request);
    PickupRequest findById(@Param("id") UUID id);
    List<PickupRequest> findByShopId(@Param("shopId") UUID shopId);
    int updateOpen(PickupRequest request);
    int cancelOpen(@Param("id") UUID id, @Param("sellerId") UUID sellerId);
    int insertShipment(@Param("pickupRequestId") UUID pickupRequestId,
                       @Param("shipmentId") UUID shipmentId);
    int deleteShipments(@Param("pickupRequestId") UUID pickupRequestId);
    List<UUID> findShipmentIds(@Param("pickupRequestId") UUID pickupRequestId);
    List<PickupRequest> findOpenByHubAndZone(@Param("hubId") UUID hubId,
                                             @Param("zoneId") UUID zoneId);
    List<PickupRequest> findNewForSession(@Param("hubId") UUID hubId,
                                          @Param("zoneId") UUID zoneId,
                                          @Param("sessionId") UUID sessionId);
    int deferForSession(@Param("sessionId") UUID sessionId,
                        @Param("pickupRequestId") UUID pickupRequestId);
    int claimOpenByHubAndZone(@Param("hubId") UUID hubId, @Param("zoneId") UUID zoneId,
                              @Param("sessionId") UUID sessionId,
                              @Param("shipperId") UUID shipperId);
    List<UUID> findIdsBySession(@Param("sessionId") UUID sessionId);
    List<PickupRequest> findBySession(@Param("sessionId") UUID sessionId);
    List<PickupRequest> findRemainingBySession(@Param("sessionId") UUID sessionId);
    int claimOne(@Param("id") UUID id, @Param("hubId") UUID hubId,
                 @Param("zoneId") UUID zoneId, @Param("sessionId") UUID sessionId,
                 @Param("shipperId") UUID shipperId);
    int countPickedUpBySession(@Param("sessionId") UUID sessionId);
    int markPickedUpAtOriginHub(@Param("sessionId") UUID sessionId);
    int revertSession(@Param("sessionId") UUID sessionId);
    int markPickedUp(@Param("id") UUID id, @Param("shipperId") UUID shipperId,
                     @Param("sessionId") UUID sessionId);
}
