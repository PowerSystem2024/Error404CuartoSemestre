package com.ecommerce.service.interfaces;

import com.ecommerce.dto.request.CategoryRequest;
import com.ecommerce.model.entity.Category;

import java.util.List;

public interface CategoryService {

    /**
     * Obtiene todas las categorías activas ordenadas por sortOrder
     */
    List<Category> getAllActiveCategories();

    /**
     * Obtiene las categorías raíz (sin padre)
     */
    List<Category> getRootCategories();

    /**
     * Obtiene una categoría por su ID si está activa
     */
    Category getCategoryById(Long id);

    /**
     * Obtiene las subcategorías de una categoría
     */
    List<Category> getSubcategories(Long parentId);

    /**
     * Crea una nueva categoría
     */
    Category createCategory(CategoryRequest request);

    /**
     * Actualiza una categoría existente
     */
    Category updateCategory(Long id, CategoryRequest request);

    /**
     * Elimina una categoría (desactivación)
     */
    void deleteCategory(Long id);

    /**
     * Elimina permanentemente una categoría (hard delete)
     */
    void hardDeleteCategory(Long id);

    /**
     * Restaura una categoría previamente desactivada
     */
    void restoreCategory(Long id);

    /**
     * Obtiene categorías por IDs
     */
    List<Category> getCategoriesByIds(List<Long> ids);
}