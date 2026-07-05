package com.bookverse.service;

import com.bookverse.dto.request.OrderRequest;
import com.bookverse.dto.response.OrderResponse;
import com.bookverse.entity.*;
import com.bookverse.exception.BookVerseException;
import com.bookverse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookVerseException("User not found", 404));

        Order order = Order.builder()
                .user(user)
                .notes(request.getNotes())
                .status("PENDING")
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Book book = bookRepository.findById(itemReq.getBookId())
                    .orElseThrow(() -> new BookVerseException("Book not found: " + itemReq.getBookId(), 404));

            if (book.getStock() < itemReq.getQuantity()) {
                throw new BookVerseException("Insufficient stock for book: " + book.getTitle(), 400);
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(book.getPrice())
                    .build();

            order.getItems().add(item);
            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            // Deduct stock
            book.setStock(book.getStock() - itemReq.getQuantity());
            bookRepository.save(book);
        }

        order.setTotalPrice(total);
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BookVerseException("Order not found: " + id, 404));

        if (!order.getUser().getId().equals(userId)) {
            throw new BookVerseException("Access denied to this order", 403);
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BookVerseException("Order not found: " + id, 404));

        if (!order.getUser().getId().equals(userId)) {
            throw new BookVerseException("Access denied to this order", 403);
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new BookVerseException("Only PENDING orders can be cancelled. Current: " + order.getStatus(), 400);
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            book.setStock(book.getStock() + item.getQuantity());
            bookRepository.save(book);
        }

        order.setStatus("CANCELLED");
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .bookId(i.getBook().getId())
                        .bookTitle(i.getBook().getTitle())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
