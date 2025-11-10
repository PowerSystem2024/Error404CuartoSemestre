package com.ecommerce.controller.catalog;

import com.ecommerce.model.entity.Category;
import com.ecommerce.service.interfaces.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Endpoints para gestión de categorías")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Obtener categorías", description = "Obtiene todas las categorías activas")
    public ResponseEntity<List<Category>> getCategories() {
        List<Category> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/root")
    @Operation(summary = "Obtener categorías raíz", description = "Obtiene las categorías principales (sin padre)")
    public ResponseEntity<List<Category>> getRootCategories() {
        List<Category> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID", description = "Obtiene los detalles de una categoría específica")
    public ResponseEntity<Category> getCategory(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/{id}/subcategories")
    @Operation(summary = "Obtener subcategorías", description = "Obtiene las subcategorías de una categoría específica")
    public ResponseEntity<List<Category>> getSubcategories(@PathVariable Long id) {
        List<Category> subcategories = categoryService.getSubcategories(id);
        return ResponseEntity.ok(subcategories);
    }

}
