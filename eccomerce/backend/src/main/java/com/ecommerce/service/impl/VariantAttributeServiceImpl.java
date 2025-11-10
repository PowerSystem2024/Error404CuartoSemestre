package com.ecommerce.service.impl;

import com.ecommerce.dto.request.CategoryVariantConfigRequest;
import com.ecommerce.dto.request.VariantAttributeRequest;
import com.ecommerce.dto.response.VariantAttributeResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.entity.*;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.CategoryVariantAttributeRepository;
import com.ecommerce.repository.VariantAttributeRepository;
import com.ecommerce.service.VariantAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VariantAttributeServiceImpl implements VariantAttributeService {

    private final VariantAttributeRepository attributeRepository;
    private final CategoryVariantAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public VariantAttributeResponse createAttribute(VariantAttributeRequest request) {
        if (attributeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Attribute with name '" + request.getName() + "' already exists");
        }

        VariantAttribute.AttributeType type = VariantAttribute.AttributeType.valueOf(request.getType().toUpperCase());

        VariantAttribute attribute = VariantAttribute.builder()
                .name(request.getName())
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getName())
                .type(type)
                .unit(request.getUnit())
                // .options(request.getOptions())
                .required(request.getRequired() != null ? request.getRequired() : false)
                .global(request.getGlobal() != null ? request.getGlobal() : false)
                .active(true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        attribute = attributeRepository.save(attribute);

        // Handle category relationships if not global
        if (request.getGlobal() != null && !request.getGlobal() && request.getCategoryIds() != null
                && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

                CategoryVariantAttribute categoryAttribute = CategoryVariantAttribute.builder()
                        .category(category)
                        .attribute(attribute)
                        .required(attribute.getRequired())
                        .sortOrder(attribute.getSortOrder())
                        .active(true)
                        .build();

                categoryAttributeRepository.save(categoryAttribute);
            }
        }

        return mapToResponse(attribute);
    }

    @Override
    public VariantAttributeResponse updateAttribute(Long id, VariantAttributeRequest request) {
        VariantAttribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variant attribute not found with id: " + id));

        // Check name uniqueness if changed
        if (!attribute.getName().equals(request.getName()) && attributeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Attribute with name '" + request.getName() + "' already exists");
        }

        VariantAttribute.AttributeType type = VariantAttribute.AttributeType.valueOf(request.getType().toUpperCase());

        boolean isNowGlobal = request.getGlobal() != null ? request.getGlobal() : attribute.getGlobal();

        attribute.setName(request.getName());
        attribute.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getName());
        attribute.setType(type);
        attribute.setUnit(request.getUnit());
        attribute.setRequired(request.getRequired() != null ? request.getRequired() : attribute.getRequired());
        attribute.setGlobal(isNowGlobal);
        attribute.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : attribute.getSortOrder());

        attribute = attributeRepository.save(attribute);

        // Handle category relationships
        if (isNowGlobal) {
            // If now global, remove all category relationships
            List<CategoryVariantAttribute> existingRelations = categoryAttributeRepository
                    .findByAttributeIdAndActiveTrue(attribute.getId());
            for (CategoryVariantAttribute relation : existingRelations) {
                relation.setActive(false);
                categoryAttributeRepository.save(relation);
            }
        } else if (request.getCategoryIds() != null) {
            // If not global, update category relationships
            List<CategoryVariantAttribute> existingRelations = categoryAttributeRepository
                    .findByAttributeIdAndActiveTrue(attribute.getId());

            // Deactivate relations not in the new categoryIds
            for (CategoryVariantAttribute relation : existingRelations) {
                if (!request.getCategoryIds().contains(relation.getCategory().getId())) {
                    relation.setActive(false);
                    categoryAttributeRepository.save(relation);
                }
            }

            // Add new relations
            for (Long categoryId : request.getCategoryIds()) {
                if (!categoryAttributeRepository.existsByCategoryIdAndAttributeId(categoryId, attribute.getId())) {
                    Category category = categoryRepository.findById(categoryId)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Category not found with id: " + categoryId));

                    CategoryVariantAttribute categoryAttribute = CategoryVariantAttribute.builder()
                            .category(category)
                            .attribute(attribute)
                            .required(attribute.getRequired())
                            .sortOrder(attribute.getSortOrder())
                            .active(true)
                            .build();

                    categoryAttributeRepository.save(categoryAttribute);
                }
            }
        }

        return mapToResponse(attribute);
    }

    @Override
    public void deleteAttribute(Long id) {
        VariantAttribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variant attribute not found with id: " + id));

        // Soft delete - just deactivate
        attribute.setActive(false);
        attributeRepository.save(attribute);
    }

    @Override
    @Transactional(readOnly = true)
    public VariantAttributeResponse getAttribute(Long id) {
        VariantAttribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variant attribute not found with id: " + id));
        return mapToResponse(attribute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantAttributeResponse> getAllAttributes() {
        try {
            List<VariantAttribute> attributes = attributeRepository.findByActiveTrueOrderBySortOrder();
            return attributes.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Log the error for debugging
            // Error logging removed for production mode
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantAttributeResponse> getGlobalAttributes() {
        return attributeRepository.findByGlobalTrueAndActiveTrueOrderBySortOrder()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantAttributeResponse> getAvailableAttributesForCategory(Long categoryId) {
        return attributeRepository.findAvailableAttributesForCategory(categoryId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void configureCategoryAttributes(CategoryVariantConfigRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        // Remove existing configuration
        categoryAttributeRepository.deleteByCategoryId(request.getCategoryId());

        // Add new configuration
        for (CategoryVariantConfigRequest.VariantAttributeAssignment assignment : request.getAttributes()) {
            VariantAttribute attribute = attributeRepository.findById(assignment.getAttributeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant attribute not found with id: " + assignment.getAttributeId()));

            CategoryVariantAttribute categoryAttribute = CategoryVariantAttribute.builder()
                    .category(category)
                    .attribute(attribute)
                    .required(assignment.getRequired() != null ? assignment.getRequired() : false)
                    .active(assignment.getActive() != null ? assignment.getActive() : true)
                    .sortOrder(assignment.getSortOrder() != null ? assignment.getSortOrder() : 0)
                    .customDisplayName(assignment.getCustomDisplayName())
                    .customOptions(assignment.getCustomOptions())
                    .build();

            categoryAttributeRepository.save(categoryAttribute);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantAttributeResponse> getCategoryAttributes(Long categoryId) {
        List<CategoryVariantAttribute> categoryAttributes = categoryAttributeRepository
                .findByCategoryIdAndActiveTrueOrderBySortOrder(categoryId);

        return categoryAttributes.stream()
                .map(cva -> {
                    VariantAttributeResponse response = mapToResponse(cva.getAttribute());
                    response.setRequired(cva.getRequired());
                    if (cva.getCustomDisplayName() != null) {
                        response.setDisplayName(cva.getCustomDisplayName());
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VariantAttribute getAttributeEntity(Long id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variant attribute not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAttributeAvailableForCategory(Long attributeId, Long categoryId) {
        VariantAttribute attribute = attributeRepository.findById(attributeId).orElse(null);
        if (attribute == null)
            return false;

        if (attribute.getGlobal())
            return true;

        return categoryAttributeRepository.existsByCategoryIdAndAttributeId(categoryId, attributeId);
    }

    private VariantAttributeResponse mapToResponse(VariantAttribute attribute) {
        return VariantAttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .displayName(attribute.getDisplayName())
                .type(attribute.getType().name())
                .unit(attribute.getUnit())
                // .options(attribute.getOptions())
                .required(attribute.getRequired())
                .global(attribute.getGlobal())
                .active(attribute.getActive())
                .sortOrder(attribute.getSortOrder())
                .createdAt(attribute.getCreatedAt())
                .updatedAt(attribute.getUpdatedAt())
                .build();
    }
}