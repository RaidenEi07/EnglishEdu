package com.sso.controller;

import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.LevelResponse;
import com.sso.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
public class LevelController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LevelResponse>>> getAllLevels() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAllLevels()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LevelResponse>> getLevel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getLevel(id)));
    }
}
