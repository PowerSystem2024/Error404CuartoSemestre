package com.ecommerce.dto.response;

import com.ecommerce.model.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private UserResponse user;
    private Integer rating;
    private String title;
    private String comment;
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
    private Boolean verifiedPurchase;
    private Boolean approved;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime approvedAt;
    private Boolean isDeleted;
}
