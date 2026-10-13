package com.karnoinventory.repository;

import com.karnoinventory.entity.InventoryReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface InventoryReservationRepo {
    int insert(InventoryReservation reservation);
    List<InventoryReservation> findByCheckoutIdForUpdate(
            @Param("checkoutId") UUID checkoutId
    );

    int markConfirmed(
            @Param("reservationId") UUID reservationId
    );
}