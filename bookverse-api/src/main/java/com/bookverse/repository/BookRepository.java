package com.bookverse.repository;

import com.bookverse.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    Page<Book> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Book> findByAuthorId(Long authorId, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE " +
           "(:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR b.category.id = :categoryId) AND " +
           "(:authorId IS NULL OR b.author.id = :authorId)")
    Page<Book> searchBooks(@Param("search") String search,
                           @Param("categoryId") Long categoryId,
                           @Param("authorId") Long authorId,
                           Pageable pageable);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.stock <= 0")
    long countOutOfStock();
}
