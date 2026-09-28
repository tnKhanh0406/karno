package com.karnohub.repository;

import com.karnohub.entity.Hub;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HubRepo {
    int insert(Hub hub);

    Optional<Hub> findById(@Param("id") UUID id);

    Optional<Hub> findByHubCode(@Param("hubCode") String hubCode);

    List<Hub> findAll();

    List<Hub> findByStatus(@Param("status") String status);
}
