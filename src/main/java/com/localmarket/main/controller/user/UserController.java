package com.localmarket.main.controller.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.localmarket.main.dto.user.FilterUsersResponse;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.service.user.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserService userService;
    // private final JwtService jwtService;
    
    // create user
    @PostMapping
    @AdminOnly
    public ResponseEntity<User> createUser(@RequestBody User user, @RequestHeader("Authorization") String token) {
        User savedUser = userService.saveUser(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    // Get Users with optional role filter
    @GetMapping
    @AdminOnly
    public List<FilterUsersResponse> getUsers(
            @RequestParam(required = false) Role role,
            @RequestHeader("Authorization") String token) {
        List<User> users = (role != null) 
            ? userService.findUsersByRole(role)
            : userService.getAllUsers();
            
        return users.stream()
                .map(user -> new FilterUsersResponse(
                    user.getUsername(), 
                    user.getEmail(), 
                    user.getFirstname(), 
                    user.getLastname()))
                .collect(Collectors.toList());
    }

    // Get User by ID
    @GetMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> getUserById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update User
    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user, @RequestHeader("Authorization") String token) {
        if (!userService.getUserById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        user.setUserId(id);
        User updatedUser = userService.saveUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    // Delete User
    @DeleteMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        if (!userService.getUserById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

