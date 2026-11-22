-- Composite indexes for the per-user filters the book/book-admin endpoints and the
-- agent/recommendation/insight queries run on every request (all filter or group on
-- these columns): user_id alone, user_id+genre, user_id+status, and user_id+created_at
-- for the default "newest first" ordering (BookRepository.findByOwner, agent Sort.NEWEST).
CREATE INDEX IF NOT EXISTS idx_books_user_id ON books (user_id);
CREATE INDEX IF NOT EXISTS idx_books_user_id_genre ON books (user_id, genre);
CREATE INDEX IF NOT EXISTS idx_books_user_id_status ON books (user_id, status);
CREATE INDEX IF NOT EXISTS idx_books_user_id_created_at ON books (user_id, created_at DESC);

-- AgentRepository/RecommendationRepository filter and join on lower(genre)/lower(author)/
-- lower(title) (case-insensitive matching), never on the plain column, so the plain
-- idx_books_genre index from V1 cannot be used for those lookups and they fall back to a
-- sequential scan. Functional indexes on the lower() expressions actually used fix that.
CREATE INDEX IF NOT EXISTS idx_books_genre_lower ON books (lower(genre));
CREATE INDEX IF NOT EXISTS idx_books_author_lower ON books (lower(author));
CREATE INDEX IF NOT EXISTS idx_books_title_lower ON books (lower(title));

-- RecommendationRepository matches/joins books across users by lower(title)+lower(author)
-- together (similarUsers' self-join, and the "not exists" dedup in booksByGenres/
-- booksByAuthors/booksOwnedBy) - a composite expression index serves that pair directly.
CREATE INDEX IF NOT EXISTS idx_books_title_author_lower ON books (lower(title), lower(author));
