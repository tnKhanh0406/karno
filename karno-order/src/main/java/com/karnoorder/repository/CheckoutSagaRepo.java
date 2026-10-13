package com.karnoorder.repository;

import com.karnoorder.entity.CheckoutSaga;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CheckoutSagaRepo {
    void insert(CheckoutSaga checkoutSaga);
    int insertIfAbsent(CheckoutSaga saga);

    CheckoutSaga findByCustomerAndKey(
            @Param("customerId") UUID customerId,
            @Param("idempotencyKey") String idempotencyKey
    );

    CheckoutSaga findByIdAndCustomer(
            @Param("checkoutId") UUID checkoutId,
            @Param("customerId") UUID customerId
    );

    CheckoutSaga findByIdForUpdate(
            @Param("checkoutId") UUID checkoutId
    );

    int markOrderCommitted(
            @Param("checkoutId") UUID checkoutId
    );

    int markFailed(
            @Param("checkoutId") UUID checkoutId,
            @Param("failureCode") String failureCode,
            @Param("failureMessage") String failureMessage
    );

    int markCompensating(
            @Param("checkoutId") UUID checkoutId,
            @Param("failureCode") String failureCode,
            @Param("failureMessage") String failureMessage
    );

    int markFailedAfterRelease(
            @Param("checkoutId") UUID checkoutId
    );

    int markCompleted(
            @Param("checkoutId") UUID checkoutId
    );
}
