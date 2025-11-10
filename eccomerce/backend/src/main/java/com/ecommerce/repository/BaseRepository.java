package com.ecommerce.repository;

import com.ecommerce.model.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL AND e.active = true")
    List<T> findAllActive();

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deletedAt IS NULL AND e.active = true")
    Optional<T> findActiveById(ID id);

    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NOT NULL")
    List<T> findAllDeleted();

    @Modifying
    @Transactional
    @Query("UPDATE #{#entityName} e SET e.deletedAt = CURRENT_TIMESTAMP, e.deletedBy = :deletedBy, e.active = false WHERE e.id = :id AND e.deletedAt IS NULL")
    int softDeleteById(ID id, String deletedBy);

    @Modifying
    @Transactional
    @Query("UPDATE #{#entityName} e SET e.deletedAt = NULL, e.deletedBy = NULL, e.active = true WHERE e.id = :id AND e.deletedAt IS NOT NULL")
    int restoreById(ID id);
}