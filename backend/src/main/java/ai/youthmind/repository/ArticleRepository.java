package ai.youthmind.repository;

import ai.youthmind.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    
    List<Article> findByPublishedTrue();
    
    List<Article> findByPublishedTrueOrderByCreatedAtDesc();
    
    Page<Article> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.intro) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND a.published = true " +
           "ORDER BY a.createdAt DESC")
    Page<Article> searchPublishedArticles(@Param("query") String query, Pageable pageable);
}
