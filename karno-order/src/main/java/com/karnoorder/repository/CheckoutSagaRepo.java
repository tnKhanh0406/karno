package com.karnoorder.repository;

import com.karnoorder.entity.CheckoutSaga;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckoutSagaRepo {
    void insert(CheckoutSaga checkoutSaga);
}
