package com.bookverse.controller;

import com.bookverse.dto.response.ApiResponse;
import com.bookverse.entity.*;
import com.bookverse.exception.BookVerseException;
import com.bookverse.repository.*;
import com.bookverse.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wishlist & Inventory", description = "Wishlist and inventory management")
public class WishlistInventoryController {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();
    }

    // ── Wishlist ────────────────────────────────────────────────────────────────

    @GetMapping("/wishlist")
    @Operation(summary = "Get my wishlist")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWishlist(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        List<Map<String, Object>> items = wishlistRepository.findByUserId(userId).stream()
                .map(w -> Map.<String, Object>of(
                        "id", w.getId(),
                        "bookId", w.getBook().getId(),
                        "bookTitle", w.getBook().getTitle(),
                        "price", w.getBook().getPrice(),
                        "addedAt", w.getAddedAt().toString()
                )).toList();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/wishlist/{bookId}")
    @Operation(summary = "Add book to wishlist")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookId) {
        Long userId = resolveUserId(userDetails);
        if (wishlistRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new BookVerseException("Book already in wishlist", 409);
        }
        User user = userRepository.findById(userId).orElseThrow();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookVerseException("Book not found: " + bookId, 404));

        Wishlist w = wishlistRepository.save(Wishlist.builder().user(user).book(book).build());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(Map.of("id", w.getId(), "bookId", bookId), "Added to wishlist"));
    }

    @DeleteMapping("/wishlist/{bookId}")
    @Operation(summary = "Remove book from wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookId) {
        Long userId = resolveUserId(userDetails);
        wishlistRepository.deleteByUserIdAndBookId(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success(null, "Removed from wishlist"));
    }

    // ── Inventory ───────────────────────────────────────────────────────────────

    @GetMapping("/inventory/{bookId}")
    @Operation(summary = "Get stock for a book")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventory(@PathVariable Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookVerseException("Book not found: " + bookId, 404));
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "bookId", bookId,
                "title", book.getTitle(),
                "stock", book.getStock()
        )));
    }

    @PutMapping("/inventory/{bookId}")
    @Operation(summary = "Update book stock (Admin only)")
    public ResponseEntity<ApiResponse<Object>> updateInventory(
            @PathVariable Long bookId,
            @RequestBody Map<String, Integer> request) {
        Integer quantity = request.get("quantity");
        if (quantity == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("quantity is required"));
        }
        return ResponseEntity.ok(ApiResponse.success(bookService.updateStock(bookId, quantity), "Stock updated"));
    }
}
