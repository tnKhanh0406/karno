package com.karnoorder.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CartRepo {
    UUID findCartIdByUserId(@Param("userId") UUID userId);
}
