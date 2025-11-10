package com.ecommerce.controller.catalog;

import com.ecommerce.dto.response.VariantAttributeResponse;
import com.ecommerce.service.VariantAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/variant-attributes")
@RequiredArgsConstructor
@Tag(name = "Variant Attributes Catalog", description = "Public endpoints for variant attributes")
public class VariantAttributeController {

    private final VariantAttributeService variantAttributeService;

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get available variant attributes for a category")
    public ResponseEntity<List<VariantAttributeResponse>> getAvailableAttributesForCategory(
            @PathVariable Long categoryId) {
        List<VariantAttributeResponse> response = variantAttributeService.getAvailableAttributesForCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/global")
    @Operation(summary = "Get global variant attributes")
    public ResponseEntity<List<VariantAttributeResponse>> getGlobalAttributes() {
        List<VariantAttributeResponse> response = variantAttributeService.getGlobalAttributes();
        return ResponseEntity.ok(response);
    }
}