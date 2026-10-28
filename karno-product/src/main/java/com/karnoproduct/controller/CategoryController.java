package com.karnoproduct.controller;

import com.karnoproduct.dto.response.CategoryFilterResponse;
import com.karnoproduct.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryFilterResponse> getCategories() {
        return categoryService.getCategories();
    }
}
