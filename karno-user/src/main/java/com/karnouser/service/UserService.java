package com.karnouser.service;

import com.karnouser.dto.response.ReviewUserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<ReviewUserResponse> getUsersByIds(List<UUID> userIds);
}
