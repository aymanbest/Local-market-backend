package com.localmarket.main.service.user;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.user.UserRepository;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Create 
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // all Users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Delete 
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // filter by role
    public List<User> findUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }
}

