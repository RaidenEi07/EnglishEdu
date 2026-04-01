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

@RestController
@RequestMapping("/api/v1/admin/master-data")
@RequiredArgsConstructor
public class AdminMasterDataController {

    private final CategoryService categoryService;

    /* ── Categories ─────────────────────────────── */

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /* ── Levels ─────────────────────────────────── */

    @PostMapping("/levels")
    public ResponseEntity<ApiResponse<LevelResponse>> createLevel(
            @Valid @RequestBody CreateLevelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.createLevel(request)));
    }

    @PutMapping("/levels/{id}")
    public ResponseEntity<ApiResponse<LevelResponse>> updateLevel(
            @PathVariable Long id, @Valid @RequestBody CreateLevelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.updateLevel(id, request)));
    }

    @DeleteMapping("/levels/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLevel(@PathVariable Long id) {
        categoryService.deleteLevel(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
