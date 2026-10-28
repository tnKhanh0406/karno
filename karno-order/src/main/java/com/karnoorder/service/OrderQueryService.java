package com.karnoorder.service;

import com.karnoorder.dto.request.CustomerOrderSearchRequest;
import com.karnoorder.dto.response.CustomerOrderResponse;
import com.karnoorder.dto.response.PageResponse;

import java.util.UUID;

public interface OrderQueryService {
    PageResponse<CustomerOrderResponse> getCustomerOrders(UUID customerId, CustomerOrderSearchRequest request);
}
