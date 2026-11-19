CREATE TABLE users (
    id          UUID PRIMARY KEY,
    username    VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE books (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    author      VARCHAR(255) NOT NULL,
    genre       VARCHAR(100),
    status      VARCHAR(20) NOT NULL DEFAULT 'TO_READ'
                CHECK (status IN ('TO_READ', 'READING', 'COMPLETED')),
    price       NUMERIC(10, 2),
    pages       INTEGER,
    isbn        VARCHAR(20),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_books_user_id ON books(user_id);
CREATE INDEX idx_books_genre ON books(genre);