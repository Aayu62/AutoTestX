-- ============================================================
-- V1__init_schema.sql
-- BookVerse Database Schema
-- ============================================================

-- Users
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)        NOT NULL,
    email       VARCHAR(255)        NOT NULL UNIQUE,
    password    VARCHAR(255)        NOT NULL,
    role        VARCHAR(20)         NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Authors
CREATE TABLE IF NOT EXISTS authors (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150)        NOT NULL,
    bio         TEXT,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)        NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Books
CREATE TABLE IF NOT EXISTS books (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255)        NOT NULL,
    isbn        VARCHAR(20)         NOT NULL UNIQUE,
    description TEXT,
    price       NUMERIC(10, 2)      NOT NULL,
    stock       INTEGER             NOT NULL DEFAULT 0,
    author_id   BIGINT              NOT NULL REFERENCES authors(id) ON DELETE RESTRICT,
    category_id BIGINT              NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    published_date DATE,
    cover_image_url VARCHAR(512),
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Orders
CREATE TABLE IF NOT EXISTS orders (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(20)         NOT NULL DEFAULT 'PENDING',
    total_price NUMERIC(10, 2)      NOT NULL DEFAULT 0.00,
    notes       TEXT,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT              NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id     BIGINT              NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    quantity    INTEGER             NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10, 2)      NOT NULL
);

-- Reviews
CREATE TABLE IF NOT EXISTS reviews (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id     BIGINT              NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    rating      INTEGER             NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, book_id)
);

-- Wishlist
CREATE TABLE IF NOT EXISTS wishlists (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id     BIGINT              NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    added_at    TIMESTAMP           NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, book_id)
);

-- Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512)        NOT NULL UNIQUE,
    expiry_date TIMESTAMP           NOT NULL,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_books_author    ON books(author_id);
CREATE INDEX idx_books_category  ON books(category_id);
CREATE INDEX idx_orders_user     ON orders(user_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_reviews_book    ON reviews(book_id);
CREATE INDEX idx_wishlists_user  ON wishlists(user_id);
CREATE INDEX idx_refresh_token   ON refresh_tokens(token);
