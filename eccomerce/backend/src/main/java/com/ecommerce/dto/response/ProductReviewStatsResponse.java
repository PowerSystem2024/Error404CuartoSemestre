package com.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewStatsResponse {
    private Long productId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution;
}