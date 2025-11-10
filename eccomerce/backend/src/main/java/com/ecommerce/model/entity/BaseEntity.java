package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column
    private String updatedBy;

    @Column(nullable = false)
    private Boolean active = true;

    @Column
    private LocalDateTime deletedAt;

    @Column
    private String deletedBy;

    // Helper methods
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(String deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.active = false;
    }

    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
        this.active = true;
    }

    public void hardDelete() {
        // This should only be called by admins with proper validation
        // The actual deletion will be handled by the repository
    }
}