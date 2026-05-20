DROP TABLE IF EXISTS loans CASCADE;
DROP TABLE IF EXISTS members CASCADE;
DROP TABLE IF EXISTS books CASCADE;

CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    is_new_release BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE members (
    id VARCHAR(7) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    member_type VARCHAR(20) NOT NULL
);

CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES books(id),
    member_id VARCHAR(7) NOT NULL REFERENCES members(id),
    loan_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    rental_fee INTEGER NOT NULL DEFAULT 0,
    extended BOOLEAN NOT NULL DEFAULT FALSE
);

-- 書籍データ
INSERT INTO books (title, author, isbn, available, is_new_release) VALUES
    ('テスト駆動開発', 'Kent Beck', '978-4274217883', TRUE, FALSE),
    ('リファクタリング', 'Martin Fowler', '978-4274224546', TRUE, FALSE),
    ('Clean Code', 'Robert C. Martin', '978-4048930598', FALSE, FALSE),
    ('Effective Java', 'Joshua Bloch', '978-4621303252', TRUE, FALSE),
    ('Clean Architecture', 'Robert C. Martin', '978-4048930659', TRUE, TRUE);

-- 会員データ
INSERT INTO members (id, name, email, member_type) VALUES
    ('0000001', '赤帽太郎', 'taro@example.com', 'GENERAL'),
    ('0000002', '赤帽花子', 'hanako@example.com', 'PREMIUM');

-- 貸出データ（Clean Code を赤帽太郎が貸出中）
INSERT INTO loans (book_id, member_id, loan_date, due_date, return_date, rental_fee, extended) VALUES
    (3, '0000001', CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE + INTERVAL '7 days', NULL, 0, FALSE);
