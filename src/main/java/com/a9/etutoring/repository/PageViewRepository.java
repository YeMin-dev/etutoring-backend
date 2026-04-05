package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.PageView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PageViewRepository extends JpaRepository<PageView, UUID> {

    @Query(
        value = """
            SELECT page_path, COUNT(*) AS cnt
            FROM page_views
            WHERE viewed_at >= :fromInstant AND viewed_at < :toInstant
            GROUP BY page_path
            ORDER BY cnt DESC
            LIMIT 50
            """,
        nativeQuery = true
    )
    List<Object[]> aggregateTopPages(@Param("fromInstant") Instant fromInstant, @Param("toInstant") Instant toInstant);

    @Query(
        value = """
            SELECT u.id, u.username, u.email, COUNT(pv.id) AS cnt
            FROM page_views pv
            JOIN users u ON u.id = pv.user_id
            WHERE pv.viewed_at >= :fromInstant AND pv.viewed_at < :toInstant
            GROUP BY u.id, u.username, u.email
            ORDER BY cnt DESC
            LIMIT 50
            """,
        nativeQuery = true
    )
    List<Object[]> aggregateTopUsers(@Param("fromInstant") Instant fromInstant, @Param("toInstant") Instant toInstant);

    @Query(
        value = """
            SELECT browser, COUNT(*) AS cnt
            FROM page_views
            WHERE viewed_at >= :fromInstant AND viewed_at < :toInstant
            GROUP BY browser
            ORDER BY cnt DESC
            LIMIT 50
            """,
        nativeQuery = true
    )
    List<Object[]> aggregateBrowsers(@Param("fromInstant") Instant fromInstant, @Param("toInstant") Instant toInstant);
}
