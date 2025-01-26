package com.localmarket.main.repository.user;

import com.localmarket.main.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.localmarket.main.entity.user.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAll();  
    Optional<User> findById(Long id); 
    void deleteById(Long id);  
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
} 