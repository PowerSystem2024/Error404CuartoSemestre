package com.ecommerce.controller.admin;

import com.ecommerce.dto.request.CategoryVariantConfigRequest;
import com.ecommerce.dto.request.VariantAttributeRequest;
import com.ecommerce.dto.response.VariantAttributeResponse;
import com.ecommerce.service.VariantAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/variant-attributes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Variant Attributes", description = "Admin endpoints for managing product variant attributes")
public class AdminVariantAttributeController {

    private final VariantAttributeService variantAttributeService;

    @PostMapping
    @Operation(summary = "Create a new variant attribute")
    public ResponseEntity<VariantAttributeResponse> createAttribute(
            @Valid @RequestBody VariantAttributeRequest request) {
        VariantAttributeResponse response = variantAttributeService.createAttribute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing variant attribute")
    public ResponseEntity<VariantAttributeResponse> updateAttribute(
            @PathVariable Long id,
            @Valid @RequestBody VariantAttributeRequest request) {
        VariantAttributeResponse response = variantAttributeService.updateAttribute(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a variant attribute (soft delete)")
    public ResponseEntity<Void> deleteAttribute(@PathVariable Long id) {
        variantAttributeService.deleteAttribute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a variant attribute by ID")
    public ResponseEntity<VariantAttributeResponse> getAttribute(@PathVariable Long id) {
        VariantAttributeResponse response = variantAttributeService.getAttribute(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all variant attributes")
    public ResponseEntity<List<VariantAttributeResponse>> getAllAttributes() {
        List<VariantAttributeResponse> response = variantAttributeService.getAllAttributes();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/global")
    @Operation(summary = "Get global variant attributes")
    public ResponseEntity<List<VariantAttributeResponse>> getGlobalAttributes() {
        List<VariantAttributeResponse> response = variantAttributeService.getGlobalAttributes();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get available attributes for a specific category")
    public ResponseEntity<List<VariantAttributeResponse>> getAvailableAttributesForCategory(
            @PathVariable Long categoryId) {
        List<VariantAttributeResponse> response = variantAttributeService.getAvailableAttributesForCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/category-config")
    @Operation(summary = "Configure variant attributes for a category")
    public ResponseEntity<Void> configureCategoryAttributes(@Valid @RequestBody CategoryVariantConfigRequest request) {
        variantAttributeService.configureCategoryAttributes(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/category-config/{categoryId}")
    @Operation(summary = "Get configured attributes for a category")
    public ResponseEntity<List<VariantAttributeResponse>> getCategoryAttributes(@PathVariable Long categoryId) {
        List<VariantAttributeResponse> response = variantAttributeService.getCategoryAttributes(categoryId);
        return ResponseEntity.ok(response);
    }
}