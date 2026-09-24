package com.cinevora.repository;

import com.cinevora.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.username) = lower(:username)")
    Optional<User> findByUsernameForProfileCreation(@Param("username") String username);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmailVerificationTokenHash(String tokenHash);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    long countByActiveTrue();
    @Query(value = "select u from User u where (:q = '' or lower(u.username) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%')) or lower(u.fullName) like lower(concat('%', :q, '%'))) and (:active is null or u.active = :active)", countQuery = "select count(u) from User u where (:q = '' or lower(u.username) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%')) or lower(u.fullName) like lower(concat('%', :q, '%'))) and (:active is null or u.active = :active)")
    Page<User> searchAdmin(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
