package com.karnoshipping.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface ProcessedEventRepo {
    int insertIfAbsent(@Param("eventId") UUID eventId,
                       @Param("consumerName") String consumerName,
                       @Param("eventType") String eventType);
}
