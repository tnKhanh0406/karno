package com.karnouser.repository;

import com.karnouser.dto.response.ShopInformationResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface ShopRepo {
    ShopInformationResponse findShopInformationById(@Param("shopId") UUID shopId);
}
