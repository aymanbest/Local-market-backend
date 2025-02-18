package com.localmarket.main.repository.user;

import com.localmarket.main.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.localmarket.main.entity.user.Role;
import java.time.LocalDateTime;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAll();  
    Optional<User> findById(Long id); 
    void deleteById(Long id);  
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Page<User> findByRole(Role role, Pageable pageable);
    List<User> findByRole(Role role);
    long countByCreatedAtBefore(LocalDateTime date);
    long countByRoleAndCreatedAtBetween(Role role, LocalDateTime start, LocalDateTime end);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByLastLoginBetween(LocalDateTime start, LocalDateTime end);
} 