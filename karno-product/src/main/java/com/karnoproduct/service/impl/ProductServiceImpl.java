package com.karnoproduct.service.impl;

import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductListItemResponse;
import com.karnoproduct.mapper.ProductMapper;
import com.karnoproduct.repository.ProductRepo;
import com.karnoproduct.repository.projection.ProductListProjection;
import com.karnoproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Override
    public Mono<PageResponse<ProductListItemResponse>> getProductList(ProductSearchRequest request) {
        int page = request.page() != null? Math.max(request.page(), 0) : DEFAULT_PAGE;
        int size = request.size() != null ? Math.min(Math.max(request.size(), 1), MAX_SIZE) : DEFAULT_SIZE;
        int offset = page * size;

        Mono<List<ProductListProjection>> productsMono = Mono.fromCallable(() ->
                        productRepo.searchProducts(request, offset, size)
                ).subscribeOn(Schedulers.boundedElastic());
        Mono<Long> totalElementsMono = Mono.fromCallable(() ->
                        productRepo.countProducts(request)
                ).subscribeOn(Schedulers.boundedElastic());
        return Mono.zip(productsMono, totalElementsMono)
                .map(tuple -> {
                    List<ProductListProjection> products = tuple.getT1();
                    Long totalElements = tuple.getT2();
                    List<ProductListItemResponse> productResponses = products.stream()
                            .map(productMapper::toListItemResponse)
                            .toList();
                    int totalPages = (int) Math.ceil((double) totalElements / size);
                    return new PageResponse<>(productResponses, page, size, totalElements, totalPages);
                });
    }
}
