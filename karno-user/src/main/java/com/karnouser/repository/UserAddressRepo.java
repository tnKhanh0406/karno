package com.karnouser.repository;

import com.karnouser.dto.response.AddressResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface UserAddressRepo {
    AddressResponse findAddressById(@Param("id") UUID id);
}
