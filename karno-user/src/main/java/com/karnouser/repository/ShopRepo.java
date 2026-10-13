package com.karnouser.repository;

import com.karnouser.dto.response.ShopInformationResponse;
import com.karnouser.dto.response.ShopLocationResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface ShopRepo {
    ShopInformationResponse findShopInformationById(@Param("shopId") UUID shopId);
    List<ShopInformationResponse> findShopInformationByIds(@Param("shopIds") List<UUID> shopIds);
    List<ShopLocationResponse> findShopLocationsByIds(@Param("shopIds") Set<UUID> shopIds);
}
