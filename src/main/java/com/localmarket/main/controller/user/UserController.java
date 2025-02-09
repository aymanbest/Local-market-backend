package com.localmarket.main.controller.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.localmarket.main.dto.user.FilterUsersResponse;
import com.localmarket.main.dto.user.GetAllUsersResponse;
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
import com.localmarket.main.dto.user.PasswordChangeRequest;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    
    @Operation(summary = "Get users", description = "Get all users with optional role filter (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = FilterUsersResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @AdminOnly
    public List<GetAllUsersResponse> getUsers(
            @RequestParam(required = false) Role role) {
        return userService.getUsers(role);
    }


    @Operation(summary = "Get user by ID", description = "Get specific user details (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> getUserById(
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Update user", description = "Update user details (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }


    // Delete User
    @Operation(summary = "Delete user", description = "Delete user (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Change password", description = "Change user's password (requires old password)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid old password", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        String jwt = cookieUtil.getJwtFromRequest(httpRequest);
        Long userId = jwtService.extractUserId(jwt);
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}

