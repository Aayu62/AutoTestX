package com.bookverse.service;

import com.bookverse.dto.request.BookRequest;
import com.bookverse.dto.response.BookResponse;
import com.bookverse.entity.Author;
import com.bookverse.entity.Book;
import com.bookverse.entity.Category;
import com.bookverse.exception.BookVerseException;
import com.bookverse.repository.AuthorRepository;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CategoryRepository;
import com.bookverse.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(int page, int size, String sortBy, String sortDir,
                                          String search, Long categoryId, Long authorId) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Book> books = bookRepository.searchBooks(search, categoryId, authorId, pageable);
        return books.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookVerseException("Book not found with id: " + id, 404));
        return toResponse(book);
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BookVerseException("Book with ISBN already exists: " + request.getIsbn(), 409);
        }
        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new BookVerseException("Author not found: " + request.getAuthorId(), 404));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BookVerseException("Category not found: " + request.getCategoryId(), 404));

        Book book = Book.builder()
                .title(request.getTitle())
                .isbn(request.getIsbn())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .author(author)
                .category(category)
                .publishedDate(request.getPublishedDate())
                .coverImageUrl(request.getCoverImageUrl())
                .build();

        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookVerseException("Book not found with id: " + id, 404));

        // ISBN conflict check (allow same book updating with its own ISBN)
        bookRepository.findByIsbn(request.getIsbn())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BookVerseException("ISBN already in use: " + request.getIsbn(), 409);
                    }
                });

        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new BookVerseException("Author not found: " + request.getAuthorId(), 404));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BookVerseException("Category not found: " + request.getCategoryId(), 404));

        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setAuthor(author);
        book.setCategory(category);
        book.setPublishedDate(request.getPublishedDate());
        book.setCoverImageUrl(request.getCoverImageUrl());

        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookVerseException("Book not found with id: " + id, 404);
        }
        bookRepository.deleteById(id);
    }

    @Transactional
    public BookResponse updateStock(Long id, Integer quantity) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookVerseException("Book not found with id: " + id, 404));
        if (book.getStock() + quantity < 0) {
            throw new BookVerseException("Insufficient stock. Current: " + book.getStock(), 400);
        }
        book.setStock(book.getStock() + quantity);
        return toResponse(bookRepository.save(book));
    }

    private BookResponse toResponse(Book book) {
        Double avgRating = reviewRepository.findAverageRatingByBookId(book.getId());
        long reviewCount = reviewRepository.countByBookId(book.getId());

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .price(book.getPrice())
                .stock(book.getStock())
                .author(BookResponse.AuthorInfo.builder()
                        .id(book.getAuthor().getId())
                        .name(book.getAuthor().getName())
                        .build())
                .category(BookResponse.CategoryInfo.builder()
                        .id(book.getCategory().getId())
                        .name(book.getCategory().getName())
                        .build())
                .publishedDate(book.getPublishedDate())
                .coverImageUrl(book.getCoverImageUrl())
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
