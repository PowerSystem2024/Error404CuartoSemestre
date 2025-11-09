package com.ecommerce.service.interfaces;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    /**
     * Obtiene una lista paginada de productos activos
     */
    Page<Product> getAllActiveProducts(Pageable pageable);

    /**
     * Obtiene una lista paginada de todos los productos para administradores
     */
    Page<Product> getAllProductsForAdmin(Pageable pageable);

    /**
     * Busca productos para administradores (todos los productos, sin filtrar por
     * activo)
     */
    Page<Product> searchProductsForAdmin(String query, Pageable pageable);

    /**
     * Obtiene un producto por su ID si está activo
     */
    Product getProductById(Long id);

    /**
     * Busca productos por nombre o descripción
     */
    Page<Product> searchProducts(String query, Pageable pageable);

    /**
     * Obtiene productos por categoría
     */
    Page<Product> getProductsByCategory(Long categoryId, Pageable pageable);

    /**
     * Obtiene productos destacados
     */
    List<Product> getFeaturedProducts();

    /**
     * Crea un nuevo producto
     */
    Product createProduct(ProductRequest request);

    /**
     * Actualiza un producto existente
     */
    Product updateProduct(Long id, ProductRequest request);

    /**
     * Elimina un producto (soft delete)
     */
    void deleteProduct(Long id);

    /**
     * Elimina permanentemente un producto (hard delete)
     */
    void hardDeleteProduct(Long id);

    /**
     * Restaura un producto previamente desactivado
     */
    void restoreProduct(Long id);

    /**
     * Obtiene productos con stock por debajo del umbral especificado
     */
    List<Product> getLowStockProducts(Integer threshold);

    /**
     * Verifica si existe un producto por ID
     */
    boolean existsById(Long id);

    /**
     * Obtiene productos por IDs
     */
    List<Product> getProductsByIds(List<Long> ids);
}