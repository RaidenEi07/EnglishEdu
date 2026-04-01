package com.sso.service;

import com.sso.dto.request.CreateCategoryRequest;
import com.sso.dto.request.CreateLevelRequest;
import com.sso.dto.response.CategoryResponse;
import com.sso.dto.response.LevelResponse;
import com.sso.entity.Category;
import com.sso.entity.Level;
import com.sso.exception.BadRequestException;
import com.sso.exception.ResourceNotFoundException;
import com.sso.repository.CategoryRepository;
import com.sso.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LevelRepository levelRepository;

    /* ── Categories ──────────────────────── */

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.getName())) {
            throw new BadRequestException("Category already exists: " + req.getName());
        }
        Category cat = Category.builder()
                .name(req.getName())
                .slug(req.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"))
                .description(req.getDescription())
                .build();
        return toCategoryResponse(categoryRepository.save(cat));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest req) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (req.getName() != null) {
            cat.setName(req.getName());
            cat.setSlug(req.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        }
        if (req.getDescription() != null) cat.setDescription(req.getDescription());
        return toCategoryResponse(categoryRepository.save(cat));
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) throw new ResourceNotFoundException("Category not found");
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        return toCategoryResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
    }

    /* ── Levels ──────────────────────── */

    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        return levelRepository.findAll().stream()
                .map(this::toLevelResponse)
                .toList();
    }

    @Transactional
    public LevelResponse createLevel(CreateLevelRequest req) {
        Level level = Level.builder()
                .name(req.getName())
                .slug(req.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"))
                .build();
        return toLevelResponse(levelRepository.save(level));
    }

    @Transactional
    public void deleteLevel(Long id) {
        if (!levelRepository.existsById(id)) throw new ResourceNotFoundException("Level not found");
        levelRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public LevelResponse getLevel(Long id) {
        return toLevelResponse(levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found")));
    }

    @Transactional
    public LevelResponse updateLevel(Long id, CreateLevelRequest req) {
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
        if (req.getName() != null) {
            level.setName(req.getName());
            level.setSlug(req.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        }
        return toLevelResponse(levelRepository.save(level));
    }

    /* ── Mapping ──────────────────────── */

    private CategoryResponse toCategoryResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .sortOrder(c.getSortOrder())
                .build();
    }

    private LevelResponse toLevelResponse(Level l) {
        return LevelResponse.builder()
                .id(l.getId())
                .name(l.getName())
                .slug(l.getSlug())
                .sortOrder(l.getSortOrder())
                .build();
    }
}
