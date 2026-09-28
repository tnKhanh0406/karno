package com.karnoinventory.repository;

import com.karnoinventory.entity.InventoryReservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryReservationRepo {
    int insert(InventoryReservation reservation);
}