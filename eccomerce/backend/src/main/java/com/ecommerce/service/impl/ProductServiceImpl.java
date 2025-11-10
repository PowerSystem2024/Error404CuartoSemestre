package com.ecommerce.service.impl;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.entity.Category;
import com.ecommerce.model.entity.Product;
import com.ecommerce.model.entity.ProductImage;
import com.ecommerce.model.enums.ProductStatus;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.service.interfaces.ProductService;
import com.ecommerce.service.interfaces.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;
        private final CategoryService categoryService;
        private final ProductImageRepository productImageRepository;

        @Override
        @Transactional(readOnly = true)
        public Page<Product> getAllActiveProducts(Pageable pageable) {
                log.debug("Obteniendo productos activos - Página: {}, Tamaño: {}",
                                pageable.getPageNumber(), pageable.getPageSize());

                Page<Product> products = productRepository.findByStatusAndDeletedAtIsNull(ProductStatus.ACTIVE,
                                pageable);

                log.debug("Productos obtenidos exitosamente - Cantidad: {}, Total: {}",
                                products.getNumberOfElements(), products.getTotalElements());

                return products;
        }

        @Override
        @Transactional(readOnly = true)
        public Page<Product> getAllProductsForAdmin(Pageable pageable) {
                log.debug("Obteniendo TODOS los productos para admin - Página: {}, Tamaño: {}",
                                pageable.getPageNumber(), pageable.getPageSize());

                Page<Product> products = productRepository.findAllProductsForAdmin(pageable);

                log.debug("Productos obtenidos exitosamente para admin - Cantidad: {}, Total: {}",
                                products.getNumberOfElements(), products.getTotalElements());

                return products;
        }

        @Override
        @Transactional(readOnly = true)
        public Page<Product> searchProductsForAdmin(String query, Pageable pageable) {
                log.debug("Buscando productos para admin con query: {} - Página: {}, Tamaño: {}",
                                query, pageable.getPageNumber(), pageable.getPageSize());

                Page<Product> products = productRepository.searchProductsForAdmin(query, pageable);

                log.debug("Productos encontrados para admin - Cantidad: {}, Total: {}",
                                products.getNumberOfElements(), products.getTotalElements());

                return products;
        }

        @Override
        @Transactional(readOnly = true)
        public Product getProductById(Long id) {
                log.debug("Obteniendo producto por ID: {}", id);

                Product product = productRepository.findById(id)
                                .orElseThrow(() -> {
                                        log.warn("Producto no encontrado - ID: {}", id);
                                        return new ResourceNotFoundException("Producto no encontrado con ID: " + id);
                                });

                log.debug("Producto encontrado - ID: {}, Nombre: {}", id, product.getName());
                return product;
        }

        @Override
        @Transactional(readOnly = true)
        public Page<Product> searchProducts(String query, Pageable pageable) {
                log.debug("Buscando productos - Query: '{}', Página: {}, Tamaño: {}",
                                query, pageable.getPageNumber(), pageable.getPageSize());

                Page<Product> products = productRepository.searchProducts(query, pageable);

                log.debug("Búsqueda completada - Query: '{}', Resultados: {}, Total: {}",
                                query, products.getNumberOfElements(), products.getTotalElements());

                return products;
        }

        @Override
        @Transactional(readOnly = true)
        public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
                log.debug("Obteniendo productos por categoría - CategoryID: {}, Página: {}, Tamaño: {}",
                                categoryId, pageable.getPageNumber(), pageable.getPageSize());

                Page<Product> products = productRepository.findByCategoryIdAndStatusAndDeletedAtIsNull(categoryId,
                                ProductStatus.ACTIVE, pageable);

                log.debug("Productos por categoría obtenidos - CategoryID: {}, Cantidad: {}, Total: {}",
                                categoryId, products.getNumberOfElements(), products.getTotalElements());

                return products;
        }

        @Override
        @Transactional(readOnly = true)
        public List<Product> getFeaturedProducts() {
                log.debug("Obteniendo productos destacados");

                List<Product> products = productRepository
                                .findByFeaturedTrueAndStatusAndDeletedAtIsNull(ProductStatus.ACTIVE);

                log.debug("Productos destacados obtenidos - Cantidad: {}", products.size());

                return products;
        }

        @Override
        public Product createProduct(ProductRequest request) {
                log.debug("Creando nuevo producto - Nombre: '{}', SKU: '{}'",
                                request.getName(), request.getSku());

                // Validar SKU único
                if (request.getSku() != null && productRepository.existsBySkuAndIdNot(request.getSku(), 0L)) {
                        throw new IllegalArgumentException("Ya existe un producto con el SKU: " + request.getSku());
                }

                // Obtener categoría si se especifica
                Category category = null;
                if (request.getCategoryId() != null) {
                        category = categoryService.getCategoryById(request.getCategoryId());
                }

                // Crear el producto
                Product product = Product.builder()
                                .name(request.getName())
                                .description(request.getDescription())
                                .shortDescription(request.getShortDescription())
                                .price(request.getPrice())
                                .compareAtPrice(request.getCompareAtPrice())
                                .stockQuantity(request.getStockQuantity())
                                .lowStockThreshold(
                                                request.getLowStockThreshold() != null ? request.getLowStockThreshold()
                                                                : 5)
                                .sku(request.getSku())
                                .barcode(request.getBarcode())
                                .category(category)
                                .status(request.getActive() != null && request.getActive() ? ProductStatus.ACTIVE
                                                : ProductStatus.INACTIVE)
                                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                                .featuredOrder(request.getFeatured() != null && request.getFeatured()
                                                ? request.getFeaturedOrder()
                                                : null)
                                .seoTitle(request.getSeoTitle())
                                .seoDescription(request.getSeoDescription())
                                .seoKeywords(request.getSeoKeywords())
                                .build();

                Product savedProduct = productRepository.save(product);

                // Crear imagen si se proporciona
                if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
                        ProductImage productImage = ProductImage.builder()
                                        .product(savedProduct)
                                        .imageUrl(request.getImageUrl().trim())
                                        .build();
                        productImageRepository.save(productImage);
                        log.debug("Imagen creada para producto - ProductID: {}, ImageURL: '{}'",
                                        savedProduct.getId(), request.getImageUrl());
                }

                // log.info("Producto creado exitosamente - ID: {}, Nombre: '{}', SKU: '{}'",
                // savedProduct.getId(), savedProduct.getName(), savedProduct.getSku());

                return savedProduct;
        }

        @Override
        @Transactional(rollbackFor = Exception.class)
        public Product updateProduct(Long id, ProductRequest request) {
                log.debug("Actualizando producto - ID: {}, Nombre: '{}'", id, request.getName());
                log.debug("Datos del request - Price: {}, Stock: {}, CategoryId: {}, ImageUrl: '{}'",
                                request.getPrice(), request.getStockQuantity(), request.getCategoryId(),
                                request.getImageUrl());

                try {
                        Product existingProduct = getProductById(id);

                        // Validar SKU único (excluyendo el producto actual)
                        if (request.getSku() != null && productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
                                throw new IllegalArgumentException(
                                                "Ya existe un producto con el SKU: " + request.getSku());
                        }

                        // Obtener categoría si se especifica
                        Category category = null;
                        if (request.getCategoryId() != null) {
                                category = categoryService.getCategoryById(request.getCategoryId());
                        }

                        // Actualizar campos solo si no son null
                        if (request.getName() != null) {
                                existingProduct.setName(request.getName());
                        }
                        if (request.getDescription() != null) {
                                existingProduct.setDescription(request.getDescription());
                        }
                        if (request.getShortDescription() != null) {
                                existingProduct.setShortDescription(request.getShortDescription());
                        }
                        if (request.getPrice() != null) {
                                existingProduct.setPrice(request.getPrice());
                        }
                        if (request.getCompareAtPrice() != null) {
                                existingProduct.setCompareAtPrice(request.getCompareAtPrice());
                        }
                        if (request.getStockQuantity() != null) {
                                existingProduct.setStockQuantity(request.getStockQuantity());
                        }
                        if (request.getLowStockThreshold() != null) {
                                existingProduct.setLowStockThreshold(request.getLowStockThreshold());
                        }
                        if (request.getSku() != null) {
                                existingProduct.setSku(request.getSku());
                        }
                        if (request.getBarcode() != null) {
                                existingProduct.setBarcode(request.getBarcode());
                        }
                        if (category != null) {
                                existingProduct.setCategory(category);
                        }
                        if (request.getActive() != null) {
                                existingProduct.setStatus(
                                                request.getActive() ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);
                        }
                        if (request.getFeatured() != null) {
                                existingProduct.setFeatured(request.getFeatured());
                                // Si es destacado, asignar orden; si no, limpiar
                                if (request.getFeatured() && request.getFeaturedOrder() != null) {
                                        existingProduct.setFeaturedOrder(request.getFeaturedOrder());
                                } else if (!request.getFeatured()) {
                                        existingProduct.setFeaturedOrder(null);
                                }
                        }
                        if (request.getSeoTitle() != null) {
                                existingProduct.setSeoTitle(request.getSeoTitle());
                        }
                        if (request.getSeoDescription() != null) {
                                existingProduct.setSeoDescription(request.getSeoDescription());
                        }
                        if (request.getSeoKeywords() != null) {
                                existingProduct.setSeoKeywords(request.getSeoKeywords());
                        }
                        existingProduct.setUpdatedAt(LocalDateTime.now());

                        Product updatedProduct = productRepository.save(existingProduct);
                        log.info("Producto actualizado y guardado - ID: {}, Nombre: '{}'", updatedProduct.getId(),
                                        updatedProduct.getName());

                        // Manejar imagen
                        if (request.getImageUrl() != null) {
                                if (request.getImageUrl().trim().isEmpty()) {
                                        // Si la URL está vacía, eliminar todas las imágenes
                                        productImageRepository.deleteByProductId(updatedProduct.getId());
                                        log.debug("Imágenes eliminadas para producto - ProductID: {}",
                                                        updatedProduct.getId());
                                } else {
                                        // Eliminar imágenes existentes y crear nueva
                                        productImageRepository.deleteByProductId(updatedProduct.getId());
                                        ProductImage productImage = ProductImage.builder()
                                                        .product(updatedProduct)
                                                        .imageUrl(request.getImageUrl().trim())
                                                        .isPrimary(true)
                                                        .sortOrder(0)
                                                        .build();
                                        productImageRepository.save(productImage);
                                        log.debug("Imagen actualizada para producto - ProductID: {}, ImageURL: '{}'",
                                                        updatedProduct.getId(), request.getImageUrl());
                                }
                        }

                        log.info("Producto actualizado exitosamente - ID: {}, Nombre: '{}'",
                                        updatedProduct.getId(), updatedProduct.getName());

                        return updatedProduct;

                } catch (Exception e) {
                        log.error("Error al actualizar producto - ID: {}: {}", id, e.getMessage(), e);
                        throw new IllegalArgumentException("Error al actualizar producto: " + e.getMessage(), e);
                }
        }

        @Override
        public void deleteProduct(Long id) {
                log.debug("Eliminando producto (soft delete) - ID: {}", id);

                Product product = getProductById(id);

                product.setDeletedAt(LocalDateTime.now());
                product.setDeletedBy(getCurrentUserEmail());
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);

                log.info("Producto eliminado exitosamente - ID: {}, Nombre: '{}', Eliminado por: {}",
                                product.getId(), product.getName(), product.getDeletedBy());
        }

        @Override
        @Transactional(readOnly = true)
        public List<Product> getLowStockProducts(Integer threshold) {
                log.debug("Obteniendo productos con stock bajo - Umbral: {}", threshold);

                List<Product> products = productRepository
                                .findByStockQuantityLessThanEqualAndStatusAndDeletedAtIsNull(threshold,
                                                ProductStatus.ACTIVE);

                log.debug("Productos con stock bajo obtenidos - Umbral: {}, Cantidad: {}", threshold, products.size());

                return products;
        }

        @Override
        @Transactional(readOnly = true)
        public boolean existsById(Long id) {
                return productRepository.existsById(id);
        }

        @Override
        @Transactional(readOnly = true)
        public List<Product> getProductsByIds(List<Long> ids) {
                log.debug("Obteniendo productos por IDs - Cantidad: {}", ids.size());

                List<Product> products = productRepository.findAllById(ids)
                                .stream()
                                .filter(p -> p.getStatus() == ProductStatus.ACTIVE && p.getDeletedAt() == null)
                                .collect(java.util.stream.Collectors.toList());

                log.debug("Productos obtenidos por IDs - Solicitados: {}, Encontrados: {}",
                                ids.size(), products.size());

                return products;
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
        public void hardDeleteProduct(Long id) {
                log.warn("Eliminando permanentemente producto - ID: {}", id);

                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Producto no encontrado con ID: " + id));

                productRepository.delete(product);

                log.warn("Producto eliminado permanentemente - ID: {}, Nombre: '{}', Eliminado por: {}",
                                product.getId(), product.getName(), getCurrentUserEmail());
        }

        @Override
        public void restoreProduct(Long id) {
                // log.info("Restaurando producto - ID: {}", id);

                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Producto no encontrado con ID: " + id));

                if (product.getActive()) {
                        throw new IllegalStateException("El producto ya está activo");
                }

                product.setActive(true);
                product.setDeletedAt(null);
                product.setDeletedBy(null);
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);

                // log.info("Producto restaurado exitosamente - ID: {}, Nombre: '{}', Restaurado
                // por: {}",
                // product.getId(), product.getName(), getCurrentUserEmail());
        }
}