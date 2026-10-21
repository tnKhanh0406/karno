package com.karnoinventory.repository;

import com.karnoinventory.entity.InventoryReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Mapper
public interface InventoryReservationRepo {
    int insert(InventoryReservation reservation);
    List<InventoryReservation> findByCheckoutIdForUpdate(
            @Param("checkoutId") UUID checkoutId
    );

    int markConfirmed(
            @Param("reservationId") UUID reservationId
    );
    int markReleased(@Param("reservationId") UUID reservationId);
    int markExpired(@Param("reservationId") UUID reservationId);
    List<UUID> findExpiredCheckoutIds(
            @Param("now") Instant now,
            @Param("limit") int limit
    );
}
