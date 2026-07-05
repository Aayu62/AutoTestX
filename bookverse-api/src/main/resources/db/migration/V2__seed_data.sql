-- ============================================================
-- V2__seed_data.sql
-- BookVerse Seed Data for Testing
-- ============================================================

-- Seed Admin User (password: Admin@123)
-- BCrypt hash of 'Admin@123'
INSERT INTO users (name, email, password, role) VALUES
('Admin User', 'admin@bookverse.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN');

-- Seed Regular User (password: User@123)
INSERT INTO users (name, email, password, role) VALUES
('Test User', 'user@bookverse.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER');

-- Seed Authors
INSERT INTO authors (name, bio) VALUES
('George Orwell', 'English novelist, essayist, journalist and critic.'),
('J.K. Rowling', 'British author, best known for the Harry Potter series.'),
('Yuval Noah Harari', 'Israeli public intellectual, historian, and professor.'),
('Robert C. Martin', 'American software engineer and author.'),
('Martin Fowler', 'British software developer and author.');

-- Seed Categories
INSERT INTO categories (name, description) VALUES
('Fiction', 'Fictional literature and novels'),
('Science Fiction', 'Science fiction and speculative fiction'),
('Non-Fiction', 'Non-fictional works including history, science, and biography'),
('Technology', 'Books on software, programming, and technology'),
('Fantasy', 'Fantasy literature and magical realism');

-- Seed Books
INSERT INTO books (title, isbn, description, price, stock, author_id, category_id, published_date) VALUES
('1984', '978-0451524935', 'A dystopian social science fiction novel.', 9.99, 100, 1, 1, '1949-06-08'),
('Animal Farm', '978-0451526342', 'An allegorical novella.', 7.99, 80, 1, 1, '1945-08-17'),
('Harry Potter and the Philosopher''s Stone', '978-0439708180', 'First book in the Harry Potter series.', 14.99, 150, 2, 5, '1997-06-26'),
('Sapiens: A Brief History of Humankind', '978-0062316097', 'A brief history of humankind.', 16.99, 60, 3, 3, '2011-01-01'),
('Clean Code', '978-0132350884', 'A Handbook of Agile Software Craftsmanship.', 39.99, 45, 4, 4, '2008-08-01'),
('Refactoring', '978-0201633610', 'Improving the Design of Existing Code.', 44.99, 30, 5, 4, '1999-07-08');
