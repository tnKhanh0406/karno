package com.karnouser.service.impl;

import com.karnouser.dto.response.ReviewUserResponse;
import com.karnouser.repository.UserRepo;
import com.karnouser.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;

    @Override
    public List<ReviewUserResponse> getUsersByIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userRepo.findUsersByIds(userIds);
    }
}
