package com.ecommerce.repository;

import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.EmailVerificationStatus;
import com.ecommerce.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long> {

       @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL AND u.active = true")
       Optional<User> findByEmail(String email);

       @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL AND u.active = true")
       Optional<User> findByUsername(String username);

       @Query("SELECT u FROM User u WHERE (u.email = :email OR u.username = :username) AND u.deletedAt IS NULL AND u.active = true")
       Optional<User> findByEmailOrUsername(String email, String username);

       @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deletedAt IS NULL AND u.active = true")
       boolean existsByEmail(String email);

       @Query("SELECT COUNT(u) FROM User u WHERE u.active = true AND u.deletedAt IS NULL")
       long countByActiveTrue();

       @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerificationStatus = :status AND u.deletedAt IS NULL AND u.active = true")
       long countByEmailVerificationStatus(EmailVerificationStatus status);

       @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.deletedAt IS NULL AND u.active = true")
       long countByRole(UserRole role);

       @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL AND u.active = true")
       List<User> findByRole(UserRole role);

       @Query("SELECT u FROM User u LEFT JOIN u.profile p WHERE " +
                     "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
                     "u.deletedAt IS NULL AND u.active = true")
       Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                     @Param("searchTerm") String searchTerm, Pageable pageable);
}
