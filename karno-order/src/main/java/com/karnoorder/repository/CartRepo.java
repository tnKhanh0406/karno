package com.karnoorder.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CartRepo {
    String findCartIdByUserId(@Param("userId") UUID userId);
}
