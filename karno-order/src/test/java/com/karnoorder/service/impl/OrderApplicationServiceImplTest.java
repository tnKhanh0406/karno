package com.karnoorder.service.impl;

import com.karnoorder.dto.request.PackOrderRequest;
import com.karnoorder.entity.Order;
import com.karnoorder.enums.OrderStatus;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.repository.OrderRepo;
import com.karnoorder.repository.OrderStatusHistoryRepo;
import com.karnoorder.service.CheckoutPreparationService;
import com.karnoorder.service.CheckoutSagaStartTxService;
import com.karnoorder.service.OutboxEventService;
import com.karnoorder.component.CheckoutRequestFingerprint;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApplicationServiceImplTest {

    @Test
    void packOrderChangesStatusWritesHistoryAndOutbox() {
        CheckoutSagaRepo checkoutSagaRepo = mock(CheckoutSagaRepo.class);
        CheckoutPreparationService preparationService = mock(CheckoutPreparationService.class);
        CheckoutRequestFingerprint fingerprint = mock(CheckoutRequestFingerprint.class);
        CheckoutSagaStartTxService sagaStartService = mock(CheckoutSagaStartTxService.class);
        OrderRepo orderRepo = mock(OrderRepo.class);
        OrderStatusHistoryRepo historyRepo = mock(OrderStatusHistoryRepo.class);
        OutboxEventService outboxService = mock(OutboxEventService.class);
        OrderApplicationServiceImpl service = new OrderApplicationServiceImpl(
                checkoutSagaRepo, preparationService, fingerprint, sagaStartService,
                orderRepo, historyRepo, outboxService);

        UUID orderId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).shopId(shopId)
                .status(OrderStatus.CONFIRMED).build();
        when(orderRepo.findById(orderId)).thenReturn(order);
        when(orderRepo.markPacked(orderId, shopId)).thenReturn(1);
        when(historyRepo.insertTransition(any(), any(), anyString(), anyString(), any(), anyString()))
                .thenReturn(1);

        var response = service.packOrder(orderId, new PackOrderRequest(shopId, sellerId));

        assertEquals(OrderStatus.PACKED, response.status());
        assertFalse(response.alreadyPacked());
        verify(outboxService).enqueueEvent(
                org.mockito.ArgumentMatchers.eq("ORDER"),
                org.mockito.ArgumentMatchers.eq(orderId),
                org.mockito.ArgumentMatchers.eq("ORDER_PACKED"),
                org.mockito.ArgumentMatchers.eq("order.packed"), any());
    }
}
