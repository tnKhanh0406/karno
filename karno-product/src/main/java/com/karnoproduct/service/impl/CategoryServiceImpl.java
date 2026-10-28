package com.karnoproduct.service.impl;

import com.karnoproduct.dto.response.CategoryFilterResponse;
import com.karnoproduct.repository.CategoryRepo;
import com.karnoproduct.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepo categoryRepo;

    @Override
    public List<CategoryFilterResponse> getCategories() {
        return categoryRepo.findAll();
    }
}
