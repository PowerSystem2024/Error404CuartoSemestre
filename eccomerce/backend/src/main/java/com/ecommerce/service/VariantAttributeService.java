package com.ecommerce.service;

import com.ecommerce.dto.request.CategoryVariantConfigRequest;
import com.ecommerce.dto.request.VariantAttributeRequest;
import com.ecommerce.dto.response.VariantAttributeResponse;
import com.ecommerce.model.entity.VariantAttribute;

import java.util.List;

public interface VariantAttributeService {

    // Attribute CRUD operations
    VariantAttributeResponse createAttribute(VariantAttributeRequest request);

    VariantAttributeResponse updateAttribute(Long id, VariantAttributeRequest request);

    void deleteAttribute(Long id);

    VariantAttributeResponse getAttribute(Long id);

    List<VariantAttributeResponse> getAllAttributes();

    List<VariantAttributeResponse> getGlobalAttributes();

    List<VariantAttributeResponse> getAvailableAttributesForCategory(Long categoryId);

    // Category configuration
    void configureCategoryAttributes(CategoryVariantConfigRequest request);

    List<VariantAttributeResponse> getCategoryAttributes(Long categoryId);

    // Utility methods
    VariantAttribute getAttributeEntity(Long id);

    boolean isAttributeAvailableForCategory(Long attributeId, Long categoryId);
}