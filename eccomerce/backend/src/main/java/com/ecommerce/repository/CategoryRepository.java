package com.ecommerce.repository;

import com.ecommerce.model.entity.Category;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends BaseRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.active = true AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findByActiveTrueOrderBySortOrderAsc();

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findByParentIsNullAndActiveTrueOrderBySortOrderAsc();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.active = true AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findRootCategories();

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.parent = :parent AND c.deletedAt IS NULL AND c.active = true")
    boolean existsByNameAndParent(String name, Category parent);
}
