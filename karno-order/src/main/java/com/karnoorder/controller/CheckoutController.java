package com.karnoorder.controller;

import com.karnoorder.dto.request.CheckoutPreviewRequest;
import com.karnoorder.dto.response.CheckoutPreviewResponse;
import com.karnoorder.service.CheckoutPreparationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkouts")
public class CheckoutController {
    private final CheckoutPreparationService checkoutPreparationService;

    @PostMapping("/{customerId}/preview")
    public CheckoutPreviewResponse preview(
            @PathVariable UUID customerId,
            @Valid @RequestBody CheckoutPreviewRequest request
    ) {
        return CheckoutPreviewResponse.from(
                checkoutPreparationService.preview(customerId, request)
        );
    }
}
