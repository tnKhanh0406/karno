package com.karnouser.controller.internal;

import com.karnouser.dto.response.AddressResponse;
import com.karnouser.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/user-addresses")
@RequiredArgsConstructor
public class UserAddressInternalController {
    private final UserAddressService userAddressService;

    @GetMapping("{addressId}")
    public AddressResponse getUserAddress(@PathVariable UUID addressId) {
        return userAddressService.getUserAddress(addressId);
    }
}
