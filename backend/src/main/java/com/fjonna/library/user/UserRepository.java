package com.fjonna.library.user;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
        select new com.fjonna.library.user.UserSummary(u.id, u.username, u.email, count(b))
        from User u left join Book b on b.user = u
        group by u.id, u.username, u.email
        order by u.username
        """)
    List<UserSummary> findAllSummaries();

    @Query("""
        select new com.fjonna.library.user.UserSummary(u.id, u.username, u.email, count(b))
        from User u left join Book b on b.user = u
        group by u.id, u.username, u.email
        order by u.username
        """)
    List<UserSummary> findAllSummaries(Pageable pageable);
}
