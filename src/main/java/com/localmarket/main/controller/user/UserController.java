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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.localmarket.main.dto.error.ErrorResponse;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    @Autowired
    private UserService userService;

    
    @Operation(summary = "Get users", description = "Get all users with optional role filter (Admin only)")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = FilterUsersResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @AdminOnly
    public List<FilterUsersResponse> getUsers(
            @RequestParam(required = false) Role role,
            @RequestHeader("Authorization") String token) {
        return userService.getUsers(role);
    }

    @Operation(summary = "Get user by ID", description = "Get specific user details (Admin only)")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> getUserById(
            @PathVariable Long id, 
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Update user", description = "Update user details (Admin only)")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // Delete User
    @Operation(summary = "Delete user", description = "Delete user (Admin only)")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id, 
            @RequestHeader("Authorization") String token) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

