package com.bookverse.controller;

import com.bookverse.dto.request.ReviewRequest;
import com.bookverse.dto.response.ApiResponse;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Book review management")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a review for a book")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest request) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reviewService.addReview(userId, request), "Review added"));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete your review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = resolveUserId(userDetails);
        reviewService.deleteReview(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted"));
    }

    @GetMapping("/book/{bookId}")
    @Operation(summary = "Get all reviews for a book")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getBookReviews(bookId, page, size)));
    }

    @GetMapping("/book/{bookId}/rating")
    @Operation(summary = "Get average rating for a book")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookRating(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getBookRating(bookId)));
    }
}
