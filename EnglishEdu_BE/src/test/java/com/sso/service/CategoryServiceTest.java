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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LevelRepository levelRepository;

    @InjectMocks
    private CategoryService categoryService;

    // ── Categories ────────────────────────────────────────

    @Test
    void getAllCategories_returnsAllMapped() {
        Category cat = Category.builder().id(1L).name("IELTS").slug("ielts").sortOrder(0).build();
        when(categoryRepository.findAll()).thenReturn(List.of(cat));

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("IELTS");
        assertThat(result.get(0).getSlug()).isEqualTo("ielts");
    }

    @Test
    void createCategory_success() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("IELTS");
        req.setDescription("IELTS preparation courses");

        when(categoryRepository.existsByNameIgnoreCase("IELTS")).thenReturn(false);
        Category saved = Category.builder()
                .id(1L).name("IELTS").slug("ielts")
                .description("IELTS preparation courses").sortOrder(0).build();
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse result = categoryService.createCategory(req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("IELTS");
        assertThat(result.getSlug()).isEqualTo("ielts");
        assertThat(result.getDescription()).isEqualTo("IELTS preparation courses");
    }

    @Test
    void createCategory_throwsWhenNameExists() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("IELTS");

        when(categoryRepository.existsByNameIgnoreCase("IELTS")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Category already exists");
    }

    @Test
    void getCategory_returnsResponse() {
        Category cat = Category.builder().id(5L).name("Cambridge").slug("cambridge").sortOrder(1).build();
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));

        CategoryResponse result = categoryService.getCategory(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("Cambridge");
    }

    @Test
    void getCategory_throwsWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_updatesNameAndSlug() {
        Category cat = Category.builder().id(1L).name("Old Name").slug("old-name").sortOrder(0).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        Category saved = Category.builder().id(1L).name("New Name").slug("new-name").sortOrder(0).build();
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("New Name");

        CategoryResponse result = categoryService.updateCategory(1L, req);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getSlug()).isEqualTo("new-name");
    }

    @Test
    void updateCategory_throwsWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, new CreateCategoryRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCategory_success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_throwsWhenNotFound() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Levels ────────────────────────────────────────

    @Test
    void getAllLevels_returnsAllMapped() {
        Level level = Level.builder().id(1L).name("Beginner").slug("beginner").sortOrder(0).build();
        when(levelRepository.findAll()).thenReturn(List.of(level));

        List<LevelResponse> result = categoryService.getAllLevels();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Beginner");
        assertThat(result.get(0).getSlug()).isEqualTo("beginner");
    }

    @Test
    void createLevel_success() {
        CreateLevelRequest req = new CreateLevelRequest();
        req.setName("Beginner");

        Level saved = Level.builder().id(1L).name("Beginner").slug("beginner").sortOrder(0).build();
        when(levelRepository.save(any(Level.class))).thenReturn(saved);

        LevelResponse result = categoryService.createLevel(req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Beginner");
        assertThat(result.getSlug()).isEqualTo("beginner");
    }

    @Test
    void createLevel_slugGeneratedFromName() {
        CreateLevelRequest req = new CreateLevelRequest();
        req.setName("Upper Intermediate");

        Level saved = Level.builder().id(2L).name("Upper Intermediate").slug("upper-intermediate").sortOrder(0).build();
        when(levelRepository.save(any(Level.class))).thenReturn(saved);

        LevelResponse result = categoryService.createLevel(req);

        assertThat(result.getSlug()).isEqualTo("upper-intermediate");
    }

    @Test
    void getLevel_returnsResponse() {
        Level level = Level.builder().id(3L).name("Advanced").slug("advanced").sortOrder(2).build();
        when(levelRepository.findById(3L)).thenReturn(Optional.of(level));

        LevelResponse result = categoryService.getLevel(3L);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("Advanced");
    }

    @Test
    void getLevel_throwsWhenNotFound() {
        when(levelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getLevel(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteLevel_success() {
        when(levelRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteLevel(1L);

        verify(levelRepository).deleteById(1L);
    }

    @Test
    void deleteLevel_throwsWhenNotFound() {
        when(levelRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteLevel(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
