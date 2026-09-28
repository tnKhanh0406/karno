package com.karnohub.repository;

import com.karnohub.entity.HubConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HubConnectionRepo {
    int insert(HubConnection connection);

    Optional<HubConnection> findById(@Param("id") UUID id);

    List<HubConnection> findByFromHubId(@Param("fromHubId") UUID fromHubId);

    List<HubConnection> findAll();

    List<HubConnection> findByStatus(@Param("status") String status);
}
