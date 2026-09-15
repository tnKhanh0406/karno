package com.karnoproduct.repository;

import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.repository.projection.ProductListProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductRepo {
    List<ProductListProjection> searchProducts(@Param("request")ProductSearchRequest request,
                                               @Param("offset") Integer offset,
                                               @Param("limit") Integer limit);

    Long countProducts(@Param("request") ProductSearchRequest request);
}
