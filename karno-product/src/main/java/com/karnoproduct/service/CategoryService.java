package com.karnoproduct.service;

import com.karnoproduct.dto.response.CategoryFilterResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryFilterResponse> getCategories();
}
