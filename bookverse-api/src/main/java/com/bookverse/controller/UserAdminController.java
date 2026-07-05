package com.bookverse.controller;

import com.bookverse.dto.response.ApiResponse;
import com.bookverse.entity.User;
import com.bookverse.exception.BookVerseException;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users & Admin", description = "User management and admin dashboard")
public class UserAdminController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    // ── User endpoints ──────────────────────────────────────────────────────────

    @GetMapping("/api/users/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BookVerseException("User not found", 404));
        return ResponseEntity.ok(ApiResponse.success(userToMap(user)));
    }

    @GetMapping("/api/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BookVerseException("User not found: " + id, 404));
        return ResponseEntity.ok(ApiResponse.success(userToMap(user)));
    }

    @PutMapping("/api/users/{id}")
    @Operation(summary = "Update user name")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BookVerseException("User not found", 404));

        if (!currentUser.getId().equals(id) && !currentUser.getRole().equals("ADMIN")) {
            throw new BookVerseException("Access denied", 403);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BookVerseException("User not found: " + id, 404));

        if (request.containsKey("name")) user.setName(request.get("name"));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(userToMap(user), "User updated"));
    }

    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new BookVerseException("User not found: " + id, 404);
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────────

    @GetMapping("/api/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin dashboard statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        long totalUsers = userRepository.count();
        long totalBooks = bookRepository.count();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus("PENDING");
        long outOfStockBooks = bookRepository.countOutOfStock();

        var recentOrders = orderRepository.findAll(PageRequest.of(0, 5))
                .getContent().stream()
                .map(o -> Map.of("id", o.getId(), "status", o.getStatus(), "total", o.getTotalPrice()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalUsers", totalUsers,
                "totalBooks", totalBooks,
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "outOfStockBooks", outOfStockBooks,
                "recentOrders", recentOrders
        )));
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<ApiResponse<Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var users = userRepository.findAll(PageRequest.of(page, size)).map(this::userToMap);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    private Map<String, Object> userToMap(User user) {
        return Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "enabled", user.getEnabled(),
                "createdAt", user.getCreatedAt().toString()
        );
    }
}
