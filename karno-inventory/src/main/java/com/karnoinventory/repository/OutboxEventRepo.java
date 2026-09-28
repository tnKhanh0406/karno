package com.karnoinventory.repository;

import com.karnoinventory.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;

@Mapper
public interface OutboxEventRepo {
    int insert(OutboxEvent event);
    OutboxEvent findById(UUID id);
}
