package com.fjonna.library.book;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("select b from Book b join fetch b.user where b.user.id = :userId order by b.createdAt desc")
    List<Book> findByOwner(UUID userId);

    @Query("select b from Book b join fetch b.user order by b.createdAt desc")
    List<Book> findAllWithOwner();

    @Query("select b from Book b join fetch b.user where b.id = :id and b.user.id = :userId")
    Optional<Book> findByIdAndOwner(Long id, UUID userId);

    @Query("select b from Book b join fetch b.user where b.id = :id")
    Optional<Book> findByIdWithOwner(Long id);
}
