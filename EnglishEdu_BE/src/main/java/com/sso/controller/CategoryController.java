package com.sso.controller;

import com.sso.dto.request.CreateCategoryRequest;
import com.sso.dto.request.CreateLevelRequest;
import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.CategoryResponse;
import com.sso.dto.response.LevelResponse;
import com.sso.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAllCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getCategory(id)));
    }
}
