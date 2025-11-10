package com.ecommerce.repository;

import com.ecommerce.model.entity.CategoryVariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryVariantAttributeRepository extends JpaRepository<CategoryVariantAttribute, Long> {

    List<CategoryVariantAttribute> findByCategoryIdAndActiveTrueOrderBySortOrder(Long categoryId);

    List<CategoryVariantAttribute> findByAttributeIdAndActiveTrue(Long attributeId);

    @Query("SELECT cva FROM CategoryVariantAttribute cva WHERE cva.category.id = :categoryId AND cva.attribute.id = :attributeId")
    CategoryVariantAttribute findByCategoryAndAttribute(@Param("categoryId") Long categoryId,
            @Param("attributeId") Long attributeId);

    @Modifying
    @Query("DELETE FROM CategoryVariantAttribute cva WHERE cva.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    boolean existsByCategoryIdAndAttributeId(Long categoryId, Long attributeId);
}