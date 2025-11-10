package com.ecommerce.service.impl;

import com.ecommerce.dto.request.CategoryRequest;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.entity.Category;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.interfaces.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllActiveCategories() {
        log.debug("Obteniendo todas las categorías activas");

        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();

        log.debug("Categorías obtenidas exitosamente - Cantidad: {}", categories.size());

        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getRootCategories() {
        log.debug("Obteniendo categorías raíz");

        List<Category> categories = categoryRepository.findRootCategories();

        log.debug("Categorías raíz obtenidas exitosamente - Cantidad: {}", categories.size());

        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        log.debug("Obteniendo categoría por ID: {}", id);

        Category category = categoryRepository.findById(id)
                .filter(Category::getActive)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada - ID: {}", id);
                    return new ResourceNotFoundException("Categoría no encontrada con ID: " + id);
                });

        log.debug("Categoría encontrada - ID: {}, Nombre: {}", id, category.getName());
        return category;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getSubcategories(Long parentId) {
        log.debug("Obteniendo subcategorías - ParentID: {}", parentId);

        List<Category> subcategories = categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(parentId);

        log.debug("Subcategorías obtenidas - ParentID: {}, Cantidad: {}", parentId, subcategories.size());

        return subcategories;
    }

    @Override
    public Category createCategory(CategoryRequest request) {
        log.debug("Creando nueva categoría - Nombre: '{}'", request.getName());

        // Validar nombre único dentro del mismo padre
        Category parent = null;
        if (request.getParentId() != null) {
            parent = getCategoryById(request.getParentId());
        }

        if (categoryRepository.existsByNameAndParent(request.getName(), parent)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + request.getName());
        }

        // Crear la categoría
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parent(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        Category savedCategory = categoryRepository.save(category);

        // log.info("Categoría creada exitosamente - ID: {}, Nombre: '{}'",
        // savedCategory.getId(), savedCategory.getName());

        return savedCategory;
    }

    @Override
    public Category updateCategory(Long id, CategoryRequest request) {
        log.debug("Actualizando categoría - ID: {}, Nombre: '{}'", id, request.getName());

        Category existingCategory = getCategoryById(id);

        // Validar nombre único (excluyendo la categoría actual)
        Category parent = null;
        if (request.getParentId() != null) {
            parent = getCategoryById(request.getParentId());
        }

        if (!existingCategory.getName().equals(request.getName()) &&
                categoryRepository.existsByNameAndParent(request.getName(), parent)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + request.getName());
        }

        // Actualizar campos
        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());
        existingCategory.setParent(parent);
        existingCategory.setSortOrder(request.getSortOrder());
        existingCategory.setActive(request.getActive());
        existingCategory.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(existingCategory);

        // log.info("Categoría actualizada exitosamente - ID: {}, Nombre: '{}'",
        // updatedCategory.getId(), updatedCategory.getName());

        return updatedCategory;
    }

    @Override
    public void deleteCategory(Long id) {
        log.debug("Eliminando categoría (desactivación) - ID: {}", id);

        Category category = getCategoryById(id);

        category.setActive(false);
        category.setDeletedAt(LocalDateTime.now());
        category.setDeletedBy(getCurrentUserEmail());
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);

        // log.info("Categoría eliminada exitosamente - ID: {}, Nombre: '{}', Eliminado
        // por: {}",
        // category.getId(), category.getName(), category.getDeletedBy());
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return categoryRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByIds(List<Long> ids) {
        log.debug("Obteniendo categorías por IDs - Cantidad: {}", ids.size());

        List<Category> categories = categoryRepository.findAllById(ids)
                .stream()
                .filter(Category::getActive)
                .collect(java.util.stream.Collectors.toList());

        log.debug("Categorías obtenidas por IDs - Solicitadas: {}, Encontradas: {}",
                ids.size(), categories.size());

        return categories;
    }

    /**
     * Obtiene el email del usuario actualmente autenticado
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "system"; // Usuario por defecto si no hay autenticación
    }

    @Override
    public void hardDeleteCategory(Long id) {
        log.warn("Eliminando permanentemente categoría - ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        // Verificar que no tenga subcategorías activas
        if (!category.getSubcategories().isEmpty()) {
            List<Category> activeSubcategories = category.getSubcategories().stream()
                    .filter(Category::getActive)
                    .collect(java.util.stream.Collectors.toList());
            if (!activeSubcategories.isEmpty()) {
                throw new IllegalStateException("No se puede eliminar la categoría porque tiene subcategorías activas");
            }
        }

        // Verificar que no tenga productos asociados
        if (!category.getProducts().isEmpty()) {
            long activeProductsCount = category.getProducts().stream()
                    .filter(product -> product.getActive() != null && product.getActive())
                    .count();
            if (activeProductsCount > 0) {
                throw new IllegalStateException("No se puede eliminar la categoría porque tiene productos asociados");
            }
        }

        categoryRepository.delete(category);

        log.warn("Categoría eliminada permanentemente - ID: {}, Nombre: '{}', Eliminado por: {}",
                category.getId(), category.getName(), getCurrentUserEmail());
    }

    @Override
    public void restoreCategory(Long id) {
        // log.info("Restaurando categoría - ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        if (category.getActive()) {
            throw new IllegalStateException("La categoría ya está activa");
        }

        category.setActive(true);
        category.setDeletedAt(null);
        category.setDeletedBy(null);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);

        // log.info("Categoría restaurada exitosamente - ID: {}, Nombre: '{}',
        // Restaurado por: {}",
        // category.getId(), category.getName(), getCurrentUserEmail());
    }
}