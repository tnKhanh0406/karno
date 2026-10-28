package com.karnoproduct.repository;

import com.karnoproduct.dto.response.CategoryFilterResponse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryRepo {
    List<CategoryFilterResponse> findAll();
}
