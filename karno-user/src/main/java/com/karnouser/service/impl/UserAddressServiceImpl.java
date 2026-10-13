package com.karnouser.service.impl;

import com.karnouser.dto.response.AddressResponse;
import com.karnouser.repository.UserAddressRepo;
import com.karnouser.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {
    private final UserAddressRepo userAddressRepo;

    @Override
    public AddressResponse getUserAddress(UUID id) {
        return userAddressRepo.findAddressById(id);
    }
}
