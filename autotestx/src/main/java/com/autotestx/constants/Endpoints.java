package com.autotestx.constants;

/**
 * Central registry of all BookVerse API endpoints.
 * Prevents magic strings in test code and makes maintenance trivial.
 */
public final class Endpoints {

    private Endpoints() {}

    // ── Authentication ──────────────────────────────────────────────────────────
    public static final String AUTH_REGISTER     = "/api/auth/register";
    public static final String AUTH_LOGIN        = "/api/auth/login";
    public static final String AUTH_LOGOUT       = "/api/auth/logout";
    public static final String AUTH_REFRESH      = "/api/auth/refresh-token";

    // ── Users ───────────────────────────────────────────────────────────────────
    public static final String USERS_ME          = "/api/users/me";
    public static final String USERS_BY_ID       = "/api/users/{id}";

    // ── Books ───────────────────────────────────────────────────────────────────
    public static final String BOOKS             = "/api/books";
    public static final String BOOKS_BY_ID       = "/api/books/{id}";

    // ── Authors ─────────────────────────────────────────────────────────────────
    public static final String AUTHORS           = "/api/authors";
    public static final String AUTHORS_BY_ID     = "/api/authors/{id}";

    // ── Categories ──────────────────────────────────────────────────────────────
    public static final String CATEGORIES        = "/api/categories";
    public static final String CATEGORIES_BY_ID  = "/api/categories/{id}";

    // ── Orders ──────────────────────────────────────────────────────────────────
    public static final String ORDERS            = "/api/orders";
    public static final String ORDERS_BY_ID      = "/api/orders/{id}";
    public static final String ORDERS_CANCEL     = "/api/orders/{id}/cancel";

    // ── Reviews ─────────────────────────────────────────────────────────────────
    public static final String REVIEWS           = "/api/reviews";
    public static final String REVIEWS_BY_ID     = "/api/reviews/{id}";
    public static final String REVIEWS_BY_BOOK   = "/api/reviews/book/{bookId}";
    public static final String REVIEWS_RATING    = "/api/reviews/book/{bookId}/rating";

    // ── Wishlist ─────────────────────────────────────────────────────────────────
    public static final String WISHLIST          = "/api/wishlist";
    public static final String WISHLIST_BY_BOOK  = "/api/wishlist/{bookId}";

    // ── Inventory ────────────────────────────────────────────────────────────────
    public static final String INVENTORY_BY_BOOK = "/api/inventory/{bookId}";

    // ── Admin ────────────────────────────────────────────────────────────────────
    public static final String ADMIN_DASHBOARD   = "/api/admin/dashboard";
    public static final String ADMIN_USERS       = "/api/admin/users";
}
