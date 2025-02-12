package com.localmarket.main.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.localmarket.main.security.CustomUserDetails;
import com.localmarket.main.dto.user.GetAllUsersResponse;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.service.user.UserService;
import com.localmarket.main.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.localmarket.main.dto.user.PasswordChangeRequest;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {
    private final UserService userService;

    @GetMapping
    @AdminOnly
    public List<GetAllUsersResponse> getUsers(
            @RequestParam(required = false) Role role) {
        return userService.getUsers(role);
    }

    @GetMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.changePassword(userDetails.getId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}

