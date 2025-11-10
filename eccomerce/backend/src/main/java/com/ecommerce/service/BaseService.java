package com.ecommerce.service;

import com.ecommerce.model.entity.BaseEntity;
import com.ecommerce.repository.BaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public abstract class BaseService<T extends BaseEntity, ID, R extends BaseRepository<T, ID>> {

    protected final R repository;

    protected BaseService(R repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<T> findAllActive() {
        return repository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Optional<T> findActiveById(ID id) {
        return repository.findActiveById(id);
    }

    @Transactional(readOnly = true)
    public List<T> findAllDeleted() {
        return repository.findAllDeleted();
    }

    @Transactional
    public boolean softDeleteById(ID id, String deletedBy) {
        return repository.softDeleteById(id, deletedBy) > 0;
    }

    @Transactional
    public boolean restoreById(ID id) {
        return repository.restoreById(id) > 0;
    }

    @Transactional
    public void hardDeleteById(ID id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsActiveById(ID id) {
        return repository.findActiveById(id).isPresent();
    }
}