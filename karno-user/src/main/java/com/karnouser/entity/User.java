package com.karnouser.entity;

import com.karnouser.enums.UserRole;
import com.karnouser.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String username;
    private String email;
    private String phone;
    private String password;
    private String fullName;
    private String avatarUrl;
    private UserStatus status;
    private UserRole role;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
