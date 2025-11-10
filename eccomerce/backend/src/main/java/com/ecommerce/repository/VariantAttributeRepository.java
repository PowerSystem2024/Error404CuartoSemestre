package com.ecommerce.repository;

import com.ecommerce.model.entity.VariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariantAttributeRepository extends JpaRepository<VariantAttribute, Long> {

    List<VariantAttribute> findByActiveTrueOrderBySortOrder();

    List<VariantAttribute> findByGlobalTrueAndActiveTrueOrderBySortOrder();

    @Query("SELECT va FROM VariantAttribute va WHERE va.global = true OR va.id IN " +
            "(SELECT cva.attribute.id FROM CategoryVariantAttribute cva WHERE cva.category.id = :categoryId AND cva.active = true) "
            +
            "ORDER BY va.sortOrder")
    List<VariantAttribute> findAvailableAttributesForCategory(@Param("categoryId") Long categoryId);

    boolean existsByName(String name);
}