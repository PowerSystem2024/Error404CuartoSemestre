package com.ecommerce.repository;

import com.ecommerce.model.entity.Address;
import com.ecommerce.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser(User user);

    List<Address> findByUserAndType(User user, String type);

    Optional<Address> findByUserAndIsDefaultTrueAndType(User user, String type);

    @Query("SELECT a FROM Address a WHERE a.user = :user AND a.type = :type ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<Address> findByUserAndTypeOrderByDefault(@Param("user") User user, @Param("type") String type);

    boolean existsByUserAndType(User user, String type);
}