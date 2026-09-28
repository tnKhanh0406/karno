package com.karnoinventory.repository;

import com.karnoinventory.entity.InventoryMovement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMovementRepo {
    int insert(InventoryMovement movement);
}
