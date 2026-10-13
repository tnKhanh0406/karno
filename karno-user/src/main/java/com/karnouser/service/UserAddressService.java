package com.karnouser.service;

import com.karnouser.dto.response.AddressResponse;

import java.util.UUID;

public interface UserAddressService {
    AddressResponse getUserAddress(UUID id);
}
