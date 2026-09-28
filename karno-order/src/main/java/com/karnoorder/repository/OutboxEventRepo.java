package com.karnoorder.repository;

import org.apache.ibatis.annotations.Mapper;
import com.karnoorder.entity.OutboxEvent;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OutboxEventRepo {
    int insert(OutboxEvent event);
    OutboxEvent findById(UUID id);
    List<OutboxEvent> findPending(@Param("limit") int limit);
    int markPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);
    int markRetry(@Param("id") UUID id, @Param("error") String error);
    int markFailed(@Param("id") UUID id, @Param("error") String error);
}
