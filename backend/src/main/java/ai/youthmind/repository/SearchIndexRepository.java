package ai.youthmind.repository;

import ai.youthmind.entity.SearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, UUID> {
    
    Optional<SearchIndex> findByEntityTypeAndEntityId(SearchIndex.EntityType entityType, UUID entityId);
    
    List<SearchIndex> findByEntityType(SearchIndex.EntityType entityType);
    
    void deleteByEntityTypeAndEntityId(SearchIndex.EntityType entityType, UUID entityId);
    
    @Query(value = "SELECT si.entity_id FROM search_index si WHERE si.entity_type = :entityType AND si.search_vector @@ plainto_tsquery('english', :query) ORDER BY ts_rank(si.search_vector, plainto_tsquery('english', :query)) DESC", nativeQuery = true)
    List<UUID> searchEntitiesByQuery(@Param("entityType") String entityType, @Param("query") String query);
    
    @Query(value = "SELECT si.entity_id FROM search_index si WHERE si.entity_type = :entityType AND si.search_vector @@ plainto_tsquery('english', :query) ORDER BY ts_rank(si.search_vector, plainto_tsquery('english', :query)) DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<UUID> searchEntitiesByQueryWithPagination(@Param("entityType") String entityType, 
                                                  @Param("query") String query, 
                                                  @Param("limit") int limit, 
                                                  @Param("offset") int offset);
    
    @Query(value = "SELECT COUNT(*) FROM search_index si WHERE si.entity_type = :entityType AND si.search_vector @@ plainto_tsquery('english', :query)", nativeQuery = true)
    Long countSearchResults(@Param("entityType") String entityType, @Param("query") String query);
    
    @Modifying
    @Transactional
    @Query(value = "UPDATE search_index SET search_vector = to_tsvector('english', :content), content_hash = md5(:content), updated_at = CURRENT_TIMESTAMP WHERE entity_type = :entityType AND entity_id = :entityId", nativeQuery = true)
    void updateSearchVector(@Param("entityType") String entityType, 
                           @Param("entityId") UUID entityId, 
                           @Param("content") String content);
    
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO search_index (entity_type, entity_id, search_vector, content_hash) VALUES (:entityType, :entityId, to_tsvector('english', :content), md5(:content)) ON CONFLICT (entity_type, entity_id) DO UPDATE SET search_vector = to_tsvector('english', :content), content_hash = md5(:content), updated_at = CURRENT_TIMESTAMP", nativeQuery = true)
    void upsertSearchVector(@Param("entityType") String entityType, 
                           @Param("entityId") UUID entityId, 
                           @Param("content") String content);
    
    @Query("SELECT si FROM SearchIndex si WHERE si.contentHash != :expectedHash AND si.entityType = :entityType AND si.entityId = :entityId")
    Optional<SearchIndex> findOutdatedIndex(@Param("entityType") SearchIndex.EntityType entityType, 
                                           @Param("entityId") UUID entityId, 
                                           @Param("expectedHash") String expectedHash);
}
