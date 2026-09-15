package com.karnouser.repository;

import com.karnouser.dto.response.ReviewUserResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface UserRepo {
    List<ReviewUserResponse> findUsersByIds(@Param("userIds") List<UUID> userIds);
}
