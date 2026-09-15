package com.karnouser.controller.internal;

import com.karnouser.dto.request.UserUUIDRequest;
import com.karnouser.dto.response.ReviewUserResponse;
import com.karnouser.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    @PostMapping("/batch")
    public List<ReviewUserResponse> getUsersByIds(@RequestBody UserUUIDRequest request) {
        return userService.getUsersByIds(request.userIds());
    }
}
