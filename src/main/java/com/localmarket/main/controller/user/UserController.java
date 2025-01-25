package com.localmarket.main.controller.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.localmarket.main.dto.user.FilterUsersResponse;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.service.user.UserService;
import com.localmarket.main.dto.auth.RegisterRequest;

import lombok.RequiredArgsConstructor;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserService userService;

    
    // Get Users with optional role filter
    @GetMapping
    @AdminOnly
    public List<FilterUsersResponse> getUsers(
            @RequestParam(required = false) Role role,
            @RequestHeader("Authorization") String token) {
        return userService.getUsers(role);
    }

    // Get User by ID
    @GetMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> getUserById(
            @PathVariable Long id, 
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Update User
    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // Delete User
    @DeleteMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id, 
            @RequestHeader("Authorization") String token) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

