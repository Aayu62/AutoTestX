package com.bookverse.service;

import com.bookverse.dto.request.ReviewRequest;
import com.bookverse.dto.response.ApiResponse;
import com.bookverse.entity.Book;
import com.bookverse.entity.Review;
import com.bookverse.entity.User;
import com.bookverse.exception.BookVerseException;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.ReviewRepository;
import com.bookverse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Object> addReview(Long userId, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndBookId(userId, request.getBookId())) {
            throw new BookVerseException("You have already reviewed this book", 409);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookVerseException("User not found", 404));
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BookVerseException("Book not found: " + request.getBookId(), 404));

        Review review = Review.builder()
                .user(user)
                .book(book)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        Double avgRating = reviewRepository.findAverageRatingByBookId(request.getBookId());

        return Map.of(
                "id", review.getId(),
                "bookId", review.getBook().getId(),
                "userId", review.getUser().getId(),
                "rating", review.getRating(),
                "comment", review.getComment() != null ? review.getComment() : "",
                "createdAt", review.getCreatedAt().toString(),
                "averageRating", avgRating != null ? avgRating : 0.0
        );
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BookVerseException("Review not found: " + reviewId, 404));

        if (!review.getUser().getId().equals(userId)) {
            throw new BookVerseException("Access denied", 403);
        }
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getBookReviews(Long bookId, int page, int size) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookVerseException("Book not found: " + bookId, 404);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByBookId(bookId, pageable).map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "userId", r.getUser().getId(),
                "userName", r.getUser().getName(),
                "rating", r.getRating(),
                "comment", r.getComment() != null ? r.getComment() : "",
                "createdAt", r.getCreatedAt().toString()
        ));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBookRating(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookVerseException("Book not found: " + bookId, 404);
        }
        Double avg = reviewRepository.findAverageRatingByBookId(bookId);
        long count = reviewRepository.countByBookId(bookId);
        return Map.of(
                "bookId", bookId,
                "averageRating", avg != null ? avg : 0.0,
                "totalReviews", count
        );
    }
}
