package com.karnoorder.service.impl;

import com.karnoorder.dto.request.CustomerOrderSearchRequest;
import com.karnoorder.dto.response.CustomerOrderItemResponse;
import com.karnoorder.dto.response.CustomerOrderResponse;
import com.karnoorder.dto.response.PageResponse;
import com.karnoorder.repository.OrderItemRepo;
import com.karnoorder.repository.OrderRepo;
import com.karnoorder.repository.projection.CustomerOrderProjection;
import com.karnoorder.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerOrderResponse> getCustomerOrders(UUID customerId, CustomerOrderSearchRequest request) {
        int page = request.page() == null ? 0 : Math.max(request.page(), 0);
        int size = request.size() == null ? DEFAULT_SIZE : Math.min(Math.max(request.size(), 1), MAX_SIZE);
        String keyword = normalizeKeyword(request.keyword());

        long totalElements = orderRepo.countCustomerOrders(customerId, request.status(), keyword);
        return buildPage(request, page, size, keyword, totalElements,
                orderRepo.findCustomerOrders(customerId, request.status(), keyword, size, page * size));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerOrderResponse> getShopOrders(UUID shopId, CustomerOrderSearchRequest request) {
        int page = request.page() == null ? 0 : Math.max(request.page(), 0);
        int size = request.size() == null ? DEFAULT_SIZE : Math.min(Math.max(request.size(), 1), MAX_SIZE);
        String keyword = normalizeKeyword(request.keyword());
        long totalElements = orderRepo.countShopOrders(shopId, request.status(), keyword);
        return buildPage(request, page, size, keyword, totalElements,
                orderRepo.findShopOrders(shopId, request.status(), keyword, size, page * size));
    }

    private PageResponse<CustomerOrderResponse> buildPage(CustomerOrderSearchRequest request,
                                                           int page, int size, String keyword,
                                                           long totalElements,
                                                           List<CustomerOrderProjection> orders) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalElements == 0 || page >= totalPages) {
            return new PageResponse<>(List.of(), page, size, totalElements, totalPages);
        }
        List<UUID> orderIds = orders.stream().map(CustomerOrderProjection::getId).toList();
        Map<UUID, List<CustomerOrderItemResponse>> itemsByOrder = orderIds.isEmpty()
                ? Collections.emptyMap()
                : orderItemRepo.findByOrderIds(orderIds).stream()
                    .collect(Collectors.groupingBy(CustomerOrderItemResponse::orderId));

        List<CustomerOrderResponse> content = orders.stream()
                .map(order -> new CustomerOrderResponse(
                        order.getId(), order.getCheckoutId(), order.getShopId(), order.getShopName(),
                        order.getStatus(), order.getSubtotal(), order.getShippingFee(), order.getTotalAmount(),
                        order.getPaymentMethod(), order.getPaymentStatus(), order.getCreatedAt(),
                        order.getTotalItems(), itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();

        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String normalized = keyword.trim();
        return normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
    }
}
