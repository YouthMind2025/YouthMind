package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Database Query Optimizer for analyzing queries and providing optimization recommendations.
 * Provides intelligent query analysis, index recommendations, and performance optimization suggestions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseQueryOptimizer {

    private final JdbcTemplate jdbcTemplate;
    private final QueryPerformanceProfiler queryProfiler;

    // Query analysis cache
    private final Map<String, QueryAnalysis> queryAnalysisCache = new ConcurrentHashMap<>();
    private final Map<String, List<OptimizationRecommendation>> recommendationsCache = new ConcurrentHashMap<>();
    
    // Index management
    private final Map<String, IndexInfo> indexRegistry = new ConcurrentHashMap<>();
    private final Map<String, IndexUsageStats> indexUsageStats = new ConcurrentHashMap<>();
    private final Map<String, List<IndexRecommendation>> indexRecommendations = new ConcurrentHashMap<>();
    
    // Background maintenance
    private final ScheduledExecutorService maintenanceScheduler = Executors.newScheduledThreadPool(2);
    
    // Configuration
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MS = 3600000; // 1 hour
    private static final long INDEX_MAINTENANCE_INTERVAL_HOURS = 24; // Daily maintenance

    /**
     * Initialize the database optimizer with index management
     */
    public void initialize() {
        try {
            // Load existing indexes
            loadExistingIndexes();
            
            // Start background maintenance tasks
            startMaintenanceTasks();
            
            // Generate initial index recommendations
            generateInitialIndexRecommendations();
            
            log.info("Database Query Optimizer initialized with {} existing indexes", indexRegistry.size());
            
        } catch (Exception e) {
            log.error("Error initializing Database Query Optimizer", e);
        }
    }

    /**
     * Create index based on recommendation
     */
    public CompletableFuture<IndexCreationResult> createIndex(IndexRecommendation recommendation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String createIndexSql = generateCreateIndexSql(recommendation);
                
                log.info("Creating index: {} on table: {}", recommendation.getIndexName(), recommendation.getTableName());
                
                long startTime = System.currentTimeMillis();
                jdbcTemplate.execute(createIndexSql);
                long creationTime = System.currentTimeMillis() - startTime;
                
                // Register the new index
                IndexInfo indexInfo = IndexInfo.builder()
                    .indexName(recommendation.getIndexName())
                    .tableName(recommendation.getTableName())
                    .columns(recommendation.getColumns())
                    .indexType(recommendation.getIndexType())
                    .createdAt(LocalDateTime.now())
                    .creationTimeMs(creationTime)
                    .build();
                
                indexRegistry.put(recommendation.getIndexName(), indexInfo);
                
                // Initialize usage stats
                indexUsageStats.put(recommendation.getIndexName(), new IndexUsageStats());
                
                log.info("Successfully created index: {} in {}ms", recommendation.getIndexName(), creationTime);
                
                return IndexCreationResult.builder()
                    .success(true)
                    .indexName(recommendation.getIndexName())
                    .creationTimeMs(creationTime)
                    .message("Index created successfully")
                    .build();
                
            } catch (Exception e) {
                log.error("Error creating index: {}", recommendation.getIndexName(), e);
                return IndexCreationResult.builder()
                    .success(false)
                    .indexName(recommendation.getIndexName())
                    .error(e.getMessage())
                    .message("Failed to create index")
                    .build();
            }
        });
    }

    /**
     * Drop index
     */
    public CompletableFuture<Boolean> dropIndex(String indexName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IndexInfo indexInfo = indexRegistry.get(indexName);
                if (indexInfo == null) {
                    log.warn("Index not found: {}", indexName);
                    return false;
                }
                
                String dropIndexSql = "DROP INDEX IF EXISTS " + indexName;
                
                log.info("Dropping index: {}", indexName);
                jdbcTemplate.execute(dropIndexSql);
                
                // Remove from registry
                indexRegistry.remove(indexName);
                indexUsageStats.remove(indexName);
                
                log.info("Successfully dropped index: {}", indexName);
                return true;
                
            } catch (Exception e) {
                log.error("Error dropping index: {}", indexName, e);
                return false;
            }
        });
    }

    /**
     * Analyze index usage and provide optimization recommendations
     */
    public List<IndexRecommendation> analyzeIndexUsage() {
        try {
            List<IndexRecommendation> recommendations = new ArrayList<>();
            
            // Get index usage statistics from database
            Map<String, IndexUsageInfo> dbIndexUsage = getDatabaseIndexUsage();
            
            for (Map.Entry<String, IndexUsageInfo> entry : dbIndexUsage.entrySet()) {
                String indexName = entry.getKey();
                IndexUsageInfo usageInfo = entry.getValue();
                
                // Analyze unused indexes
                if (usageInfo.getUsageCount() == 0 && usageInfo.getLastUsed() == null) {
                    recommendations.add(IndexRecommendation.builder()
                        .indexName(indexName)
                        .tableName(usageInfo.getTableName())
                        .recommendationType(IndexRecommendationType.DROP_UNUSED)
                        .priority("LOW")
                        .reason("Index has never been used")
                        .build());
                }
                
                // Analyze underutilized indexes
                if (usageInfo.getUsageCount() < 10 && usageInfo.getLastUsed() != null) {
                    long daysSinceLastUse = java.time.Duration.between(usageInfo.getLastUsed(), LocalDateTime.now()).toDays();
                    if (daysSinceLastUse > 30) {
                        recommendations.add(IndexRecommendation.builder()
                            .indexName(indexName)
                            .tableName(usageInfo.getTableName())
                            .recommendationType(IndexRecommendationType.REVIEW_UNDERUTILIZED)
                            .priority("MEDIUM")
                            .reason("Index has been underutilized for " + daysSinceLastUse + " days")
                            .build());
                    }
                }
            }
            
            // Generate new index recommendations based on query patterns
            recommendations.addAll(generateNewIndexRecommendations());
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("Error analyzing index usage", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get comprehensive database performance metrics
     */
    public DatabasePerformanceMetrics getDatabasePerformanceMetrics() {
        try {
            // Get query performance metrics
            QueryPerformanceProfiler.QueryPerformanceSummary querySummary = queryProfiler.getPerformanceSummary();
            
            // Get index metrics
            Map<String, IndexUsageStats> indexStats = new HashMap<>(indexUsageStats);
            
            // Get database size metrics
            DatabaseSizeMetrics sizeMetrics = getDatabaseSizeMetrics();
            
            // Get connection pool metrics (if available)
            Map<String, Object> connectionMetrics = getConnectionPoolMetrics();
            
            return DatabasePerformanceMetrics.builder()
                .totalQueries(querySummary.getTotalQueries())
                .averageQueryTime(querySummary.getAverageExecutionTime())
                .slowQueries(querySummary.getTotalSlowQueries())
                .totalIndexes(indexRegistry.size())
                .indexUsageStats(indexStats)
                .databaseSize(sizeMetrics)
                .connectionPoolMetrics(connectionMetrics)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting database performance metrics", e);
            return DatabasePerformanceMetrics.builder()
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Analyze a query and provide optimization recommendations
     */
    public List<OptimizationRecommendation> analyzeQuery(String queryName, String sqlQuery) {
        try {
            // Check cache first
            String cacheKey = generateCacheKey(queryName, sqlQuery);
            List<OptimizationRecommendation> cached = recommendationsCache.get(cacheKey);
            if (cached != null) {
                log.debug("Using cached recommendations for query: {}", queryName);
                return cached;
            }
            
            // Perform query analysis
            QueryAnalysis analysis = performQueryAnalysis(queryName, sqlQuery);
            queryAnalysisCache.put(cacheKey, analysis);
            
            // Generate recommendations
            List<OptimizationRecommendation> recommendations = generateRecommendations(analysis);
            recommendationsCache.put(cacheKey, recommendations);
            
            // Cleanup cache if needed
            cleanupCache();
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("Error analyzing query '{}': {}", queryName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get optimization recommendations for a specific entity/repository
     */
    public List<OptimizationRecommendation> getRecommendations(String entityName) {
        try {
            List<OptimizationRecommendation> recommendations = new ArrayList<>();
            
            // Generate entity-specific recommendations
            recommendations.addAll(generateIndexRecommendations(entityName));
            recommendations.addAll(generateQueryOptimizationRecommendations(entityName));
            recommendations.addAll(generateCachingRecommendations(entityName));
            recommendations.addAll(generateConnectionPoolRecommendations(entityName));
            
            // Sort by priority
            recommendations.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("Error getting recommendations for entity '{}': {}", entityName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Perform comprehensive query analysis
     */
    private QueryAnalysis performQueryAnalysis(String queryName, String sqlQuery) {
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.setQueryName(queryName);
        analysis.setSqlQuery(sqlQuery);
        analysis.setAnalysisTimestamp(LocalDateTime.now());
        
        // Analyze query complexity
        analysis.setComplexityScore(calculateComplexityScore(sqlQuery));
        
        // Analyze query patterns
        analysis.setHasJoins(sqlQuery.toLowerCase().contains("join"));
        analysis.setHasSubqueries(sqlQuery.toLowerCase().contains("select") && sqlQuery.toLowerCase().split("select").length > 2);
        analysis.setHasOrderBy(sqlQuery.toLowerCase().contains("order by"));
        analysis.setHasGroupBy(sqlQuery.toLowerCase().contains("group by"));
        analysis.setHasHaving(sqlQuery.toLowerCase().contains("having"));
        
        // Analyze WHERE clauses
        analysis.setWhereClauseCount(countWhereClauses(sqlQuery));
        
        // Analyze table usage
        analysis.setTablesUsed(extractTables(sqlQuery));
        
        // Analyze potential performance issues
        analysis.setPotentialIssues(identifyPotentialIssues(sqlQuery));
        
        return analysis;
    }

    /**
     * Generate optimization recommendations based on analysis
     */
    private List<OptimizationRecommendation> generateRecommendations(QueryAnalysis analysis) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Index recommendations
        if (analysis.isHasJoins()) {
            recommendations.add(createRecommendation(
                "Add composite indexes for JOIN operations",
                "Consider adding composite indexes on join columns to improve JOIN performance",
                "HIGH",
                Map.of("tables", analysis.getTablesUsed(), "complexity", analysis.getComplexityScore())
            ));
        }
        
        if (analysis.getWhereClauseCount() > 2) {
            recommendations.add(createRecommendation(
                "Optimize WHERE clause",
                "Consider combining multiple WHERE conditions or adding indexes on frequently filtered columns",
                "MEDIUM",
                Map.of("whereClauseCount", analysis.getWhereClauseCount())
            ));
        }
        
        // Query structure recommendations
        if (analysis.isHasSubqueries()) {
            recommendations.add(createRecommendation(
                "Consider query restructuring",
                "Subqueries can often be replaced with JOINs for better performance",
                "MEDIUM",
                Map.of("hasSubqueries", true)
            ));
        }
        
        if (analysis.getComplexityScore() > 7) {
            recommendations.add(createRecommendation(
                "Query complexity optimization",
                "Consider breaking down complex queries into simpler ones or using views",
                "HIGH",
                Map.of("complexityScore", analysis.getComplexityScore())
            ));
        }
        
        // Caching recommendations
        recommendations.add(createRecommendation(
            "Implement query result caching",
            "Consider caching results for frequently executed queries",
            "LOW",
            Map.of("queryName", analysis.getQueryName())
        ));
        
        return recommendations;
    }

    /**
     * Generate index recommendations for an entity
     */
    private List<OptimizationRecommendation> generateIndexRecommendations(String entityName) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Entity-specific index recommendations
        switch (entityName.toLowerCase()) {
            case "psychologicalstatevector":
                recommendations.add(createRecommendation(
                    "Add composite index on (user_id, vector_type, created_at)",
                    "This index will optimize queries filtering by user and vector type with time ordering",
                    "HIGH",
                    Map.of("entity", entityName, "columns", Arrays.asList("user_id", "vector_type", "created_at"))
                ));
                
                recommendations.add(createRecommendation(
                    "Add index on intensity_factor",
                    "This index will optimize queries filtering by intensity factor",
                    "MEDIUM",
                    Map.of("entity", entityName, "columns", Arrays.asList("intensity_factor"))
                ));
                
                recommendations.add(createRecommendation(
                    "Add index on source_id",
                    "This index will optimize queries filtering by source ID",
                    "MEDIUM",
                    Map.of("entity", entityName, "columns", Arrays.asList("source_id"))
                ));
                break;
                
            case "user":
                recommendations.add(createRecommendation(
                    "Add index on created_at",
                    "This index will optimize queries filtering users by creation date",
                    "MEDIUM",
                    Map.of("entity", entityName, "columns", Arrays.asList("created_at"))
                ));
                break;
                
            case "post":
                recommendations.add(createRecommendation(
                    "Add composite index on (user_id, created_at)",
                    "This index will optimize queries filtering posts by user with time ordering",
                    "HIGH",
                    Map.of("entity", entityName, "columns", Arrays.asList("user_id", "created_at"))
                ));
                break;
        }
        
        return recommendations;
    }

    /**
     * Generate query optimization recommendations
     */
    private List<OptimizationRecommendation> generateQueryOptimizationRecommendations(String entityName) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        recommendations.add(createRecommendation(
            "Use pagination for large result sets",
            "Implement LIMIT and OFFSET for queries that might return large datasets",
            "MEDIUM",
            Map.of("entity", entityName)
        ));
        
        recommendations.add(createRecommendation(
            "Optimize SELECT clauses",
            "Only select necessary columns instead of using SELECT *",
            "LOW",
            Map.of("entity", entityName)
        ));
        
        recommendations.add(createRecommendation(
            "Use prepared statements",
            "Use prepared statements to avoid SQL injection and improve performance",
            "HIGH",
            Map.of("entity", entityName)
        ));
        
        return recommendations;
    }

    /**
     * Generate caching recommendations
     */
    private List<OptimizationRecommendation> generateCachingRecommendations(String entityName) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        recommendations.add(createRecommendation(
            "Implement multi-level caching",
            "Use L1 (in-memory), L2 (Redis), and L3 (database) caching layers",
            "HIGH",
            Map.of("entity", entityName, "cacheLevels", Arrays.asList("L1", "L2", "L3"))
        ));
        
        recommendations.add(createRecommendation(
            "Cache frequently accessed data",
            "Cache user profiles, psychological states, and other frequently accessed data",
            "MEDIUM",
            Map.of("entity", entityName)
        ));
        
        recommendations.add(createRecommendation(
            "Implement intelligent cache invalidation",
            "Use event-driven cache invalidation to maintain data consistency",
            "MEDIUM",
            Map.of("entity", entityName)
        ));
        
        return recommendations;
    }

    /**
     * Generate connection pool recommendations
     */
    private List<OptimizationRecommendation> generateConnectionPoolRecommendations(String entityName) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        recommendations.add(createRecommendation(
            "Optimize connection pool settings",
            "Adjust HikariCP settings based on load patterns and database capacity",
            "MEDIUM",
            Map.of("entity", entityName, "poolSettings", Arrays.asList("maximumPoolSize", "minimumIdle", "connectionTimeout"))
        ));
        
        recommendations.add(createRecommendation(
            "Monitor connection pool metrics",
            "Track connection pool usage, wait times, and connection leaks",
            "LOW",
            Map.of("entity", entityName)
        ));
        
        return recommendations;
    }

    // Index management helper methods

    private void loadExistingIndexes() {
        try {
            // Query to get existing indexes (PostgreSQL specific)
            String sql = """
                SELECT 
                    i.indexname as index_name,
                    t.tablename as table_name,
                    pg_get_indexdef(i.indexrelid) as index_definition,
                    i.indexdef as index_type
                FROM pg_indexes i
                JOIN pg_tables t ON i.tablename = t.tablename
                WHERE i.schemaname = 'public'
                """;
            
            jdbcTemplate.query(sql, (rs) -> {
                String indexName = rs.getString("index_name");
                String tableName = rs.getString("table_name");
                String definition = rs.getString("index_definition");
                
                IndexInfo indexInfo = IndexInfo.builder()
                    .indexName(indexName)
                    .tableName(tableName)
                    .indexDefinition(definition)
                    .indexType(extractIndexType(definition))
                    .createdAt(LocalDateTime.now()) // We don't have creation time from DB
                    .build();
                
                indexRegistry.put(indexName, indexInfo);
                indexUsageStats.put(indexName, new IndexUsageStats());
            });
            
        } catch (Exception e) {
            log.error("Error loading existing indexes", e);
        }
    }

    private void startMaintenanceTasks() {
        // Schedule daily index maintenance
        maintenanceScheduler.scheduleAtFixedRate(() -> {
            try {
                performIndexMaintenance();
            } catch (Exception e) {
                log.error("Error during index maintenance", e);
            }
        }, 0, INDEX_MAINTENANCE_INTERVAL_HOURS, TimeUnit.HOURS);
        
        // Schedule index usage analysis every 6 hours
        maintenanceScheduler.scheduleAtFixedRate(() -> {
            try {
                updateIndexUsageStats();
            } catch (Exception e) {
                log.error("Error updating index usage stats", e);
            }
        }, 0, 6, TimeUnit.HOURS);
    }

    private void generateInitialIndexRecommendations() {
        // Generate recommendations for key psychological analysis tables
        String[] keyTables = {
            "psychological_state_vectors",
            "psychological_analysis_results", 
            "user_behavior_analytics",
            "posts",
            "comments",
            "user_engagements"
        };
        
        for (String table : keyTables) {
            List<IndexRecommendation> recommendations = generateTableIndexRecommendations(table);
            indexRecommendations.put(table, recommendations);
        }
    }

    private String generateCreateIndexSql(IndexRecommendation recommendation) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX ");
        
        if (recommendation.isUnique()) {
            sql.append("UNIQUE ");
        }
        
        sql.append(recommendation.getIndexName())
           .append(" ON ")
           .append(recommendation.getTableName())
           .append(" (");
        
        sql.append(String.join(", ", recommendation.getColumns()));
        sql.append(")");
        
        if (recommendation.getWhereClause() != null) {
            sql.append(" WHERE ").append(recommendation.getWhereClause());
        }
        
        return sql.toString();
    }

    private Map<String, IndexUsageInfo> getDatabaseIndexUsage() {
        Map<String, IndexUsageInfo> usageMap = new HashMap<>();
        
        try {
            // PostgreSQL specific query for index usage statistics
            String sql = """
                SELECT 
                    schemaname,
                    tablename,
                    indexname,
                    idx_tup_read,
                    idx_tup_fetch,
                    idx_scan
                FROM pg_stat_user_indexes
                WHERE schemaname = 'public'
                """;
            
            jdbcTemplate.query(sql, (rs) -> {
                String indexName = rs.getString("indexname");
                String tableName = rs.getString("tablename");
                long usageCount = rs.getLong("idx_scan");
                
                IndexUsageInfo usageInfo = IndexUsageInfo.builder()
                    .indexName(indexName)
                    .tableName(tableName)
                    .usageCount(usageCount)
                    .lastUsed(LocalDateTime.now().minusDays(1)) // Placeholder
                    .build();
                
                usageMap.put(indexName, usageInfo);
            });
            
        } catch (Exception e) {
            log.error("Error getting database index usage", e);
        }
        
        return usageMap;
    }

    private List<IndexRecommendation> generateNewIndexRecommendations() {
        List<IndexRecommendation> recommendations = new ArrayList<>();
        
        // Analyze query patterns to suggest new indexes
        Map<String, QueryAnalysis> recentQueries = queryAnalysisCache;
        
        for (QueryAnalysis analysis : recentQueries.values()) {
            if (analysis.getComplexityScore() > 5) {
                // Suggest indexes for complex queries
                for (String table : analysis.getTablesUsed()) {
                    recommendations.addAll(generateTableIndexRecommendations(table));
                }
            }
        }
        
        return recommendations;
    }

    private List<IndexRecommendation> generateTableIndexRecommendations(String tableName) {
        List<IndexRecommendation> recommendations = new ArrayList<>();
        
        switch (tableName.toLowerCase()) {
            case "psychological_state_vectors":
                recommendations.add(IndexRecommendation.builder()
                    .indexName("idx_psv_user_vector_type")
                    .tableName(tableName)
                    .columns(Arrays.asList("user_id", "vector_type", "created_at"))
                    .indexType("BTREE")
                    .recommendationType(IndexRecommendationType.CREATE_NEW)
                    .priority("HIGH")
                    .reason("Optimizes user-specific vector queries with time ordering")
                    .build());
                break;
                
            case "posts":
                recommendations.add(IndexRecommendation.builder()
                    .indexName("idx_posts_user_created")
                    .tableName(tableName)
                    .columns(Arrays.asList("user_id", "created_at"))
                    .indexType("BTREE")
                    .recommendationType(IndexRecommendationType.CREATE_NEW)
                    .priority("HIGH")
                    .reason("Optimizes user post queries with time ordering")
                    .build());
                break;
                
            case "user_engagements":
                recommendations.add(IndexRecommendation.builder()
                    .indexName("idx_ue_user_target")
                    .tableName(tableName)
                    .columns(Arrays.asList("user_id", "target_id", "engagement_type"))
                    .indexType("BTREE")
                    .recommendationType(IndexRecommendationType.CREATE_NEW)
                    .priority("MEDIUM")
                    .reason("Optimizes engagement analysis queries")
                    .build());
                break;
        }
        
        return recommendations;
    }

    private void performIndexMaintenance() {
        log.info("Starting index maintenance");
        
        try {
            // Update index statistics
            jdbcTemplate.execute("ANALYZE");
            
            // Check for index bloat and suggest REINDEX if needed
            List<String> bloatedIndexes = findBloatedIndexes();
            for (String indexName : bloatedIndexes) {
                log.warn("Index {} appears to be bloated and may need REINDEX", indexName);
            }
            
            log.info("Index maintenance completed");
            
        } catch (Exception e) {
            log.error("Error during index maintenance", e);
        }
    }

    private void updateIndexUsageStats() {
        Map<String, IndexUsageInfo> dbUsage = getDatabaseIndexUsage();
        
        for (Map.Entry<String, IndexUsageInfo> entry : dbUsage.entrySet()) {
            String indexName = entry.getKey();
            IndexUsageInfo usageInfo = entry.getValue();
            
            IndexUsageStats stats = indexUsageStats.get(indexName);
            if (stats != null) {
                stats.setUsageCount(usageInfo.getUsageCount());
                stats.setLastUsed(usageInfo.getLastUsed());
                stats.setLastUpdated(LocalDateTime.now());
            }
        }
    }

    private List<String> findBloatedIndexes() {
        List<String> bloatedIndexes = new ArrayList<>();
        
        try {
            // PostgreSQL specific query to find bloated indexes
            String sql = """
                SELECT 
                    indexname,
                    pg_size_pretty(pg_relation_size(indexrelid)) as size,
                    pg_stat_get_tuples_returned(indexrelid) as tuples_returned
                FROM pg_stat_user_indexes
                WHERE schemaname = 'public'
                AND pg_relation_size(indexrelid) > 100000000  -- 100MB
                """;
            
            jdbcTemplate.query(sql, (rs) -> {
                String indexName = rs.getString("indexname");
                bloatedIndexes.add(indexName);
            });
            
        } catch (Exception e) {
            log.error("Error finding bloated indexes", e);
        }
        
        return bloatedIndexes;
    }

    private DatabaseSizeMetrics getDatabaseSizeMetrics() {
        try {
            String sql = "SELECT pg_size_pretty(pg_database_size(current_database())) as db_size";
            String dbSize = jdbcTemplate.queryForObject(sql, String.class);
            
            return DatabaseSizeMetrics.builder()
                .databaseSize(dbSize)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting database size metrics", e);
            return DatabaseSizeMetrics.builder()
                .databaseSize("Unknown")
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    private Map<String, Object> getConnectionPoolMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Get basic connection pool info
            metrics.put("activeConnections", jdbcTemplate.getDataSource().getConnection().isValid(1));
            metrics.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error getting connection pool metrics", e);
        }
        
        return metrics;
    }

    private String extractIndexType(String definition) {
        if (definition.toLowerCase().contains("unique")) {
            return "UNIQUE";
        } else if (definition.toLowerCase().contains("btree")) {
            return "BTREE";
        } else if (definition.toLowerCase().contains("hash")) {
            return "HASH";
        } else {
            return "BTREE"; // Default
        }
    }

    // Helper methods
    private int calculateComplexityScore(String sqlQuery) {
        int score = 0;
        String lowerQuery = sqlQuery.toLowerCase();
        
        // Base score
        score += 1;
        
        // JOIN complexity
        score += (lowerQuery.split("join").length - 1) * 2;
        
        // Subquery complexity
        score += (lowerQuery.split("select").length - 1) * 2;
        
        // WHERE clause complexity
        score += countWhereClauses(sqlQuery);
        
        // ORDER BY complexity
        if (lowerQuery.contains("order by")) {
            score += 1;
        }
        
        // GROUP BY complexity
        if (lowerQuery.contains("group by")) {
            score += 2;
        }
        
        // HAVING complexity
        if (lowerQuery.contains("having")) {
            score += 2;
        }
        
        return Math.min(score, 10); // Cap at 10
    }

    private int countWhereClauses(String sqlQuery) {
        String lowerQuery = sqlQuery.toLowerCase();
        int count = 0;
        
        if (lowerQuery.contains("where")) {
            count = 1;
            // Count additional conditions (AND, OR)
            count += (lowerQuery.split(" and ").length - 1);
            count += (lowerQuery.split(" or ").length - 1);
        }
        
        return count;
    }

    private List<String> extractTables(String sqlQuery) {
        List<String> tables = new ArrayList<>();
        String lowerQuery = sqlQuery.toLowerCase();
        
        // Simple table extraction (can be enhanced)
        if (lowerQuery.contains("from ")) {
            String fromPart = lowerQuery.substring(lowerQuery.indexOf("from ") + 5);
            String[] parts = fromPart.split("\\s+");
            if (parts.length > 0) {
                tables.add(parts[0]);
            }
        }
        
        return tables;
    }

    private List<String> identifyPotentialIssues(String sqlQuery) {
        List<String> issues = new ArrayList<>();
        String lowerQuery = sqlQuery.toLowerCase();
        
        if (lowerQuery.contains("select *")) {
            issues.add("Using SELECT * can impact performance");
        }
        
        if (lowerQuery.contains("order by") && !lowerQuery.contains("limit")) {
            issues.add("ORDER BY without LIMIT can be expensive");
        }
        
        if (lowerQuery.split("join").length > 4) {
            issues.add("Multiple JOINs can impact performance");
        }
        
        return issues;
    }

    private OptimizationRecommendation createRecommendation(String title, String description, String priority, Map<String, Object> details) {
        return OptimizationRecommendation.builder()
            .title(title)
            .description(description)
            .priority(priority)
            .details(details)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private String generateCacheKey(String queryName, String sqlQuery) {
        return queryName + ":" + sqlQuery.hashCode();
    }

    private void cleanupCache() {
        if (queryAnalysisCache.size() > MAX_CACHE_SIZE) {
            // Remove oldest entries
            queryAnalysisCache.entrySet().removeIf(entry -> 
                entry.getValue().getAnalysisTimestamp().isBefore(LocalDateTime.now().minusHours(1)));
        }
        
        if (recommendationsCache.size() > MAX_CACHE_SIZE) {
            recommendationsCache.clear();
        }
    }

    // Inner classes for index management

    @lombok.Data
    @lombok.Builder
    public static class IndexInfo {
        private String indexName;
        private String tableName;
        private List<String> columns;
        private String indexType;
        private String indexDefinition;
        private LocalDateTime createdAt;
        private long creationTimeMs;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IndexUsageStats {
        private long usageCount;
        private LocalDateTime lastUsed;
        private LocalDateTime lastUpdated;
        private double efficiencyScore;
    }

    @lombok.Data
    @lombok.Builder
    public static class IndexUsageInfo {
        private String indexName;
        private String tableName;
        private long usageCount;
        private LocalDateTime lastUsed;
    }

    @lombok.Data
    @lombok.Builder
    public static class IndexRecommendation {
        private String indexName;
        private String tableName;
        private List<String> columns;
        private String indexType;
        private IndexRecommendationType recommendationType;
        private String priority;
        private String reason;
        private boolean unique;
        private String whereClause;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class IndexCreationResult {
        private boolean success;
        private String indexName;
        private long creationTimeMs;
        private String message;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class DatabasePerformanceMetrics {
        private long totalQueries;
        private double averageQueryTime;
        private long slowQueries;
        private int totalIndexes;
        private Map<String, IndexUsageStats> indexUsageStats;
        private DatabaseSizeMetrics databaseSize;
        private Map<String, Object> connectionPoolMetrics;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class DatabaseSizeMetrics {
        private String databaseSize;
        private String tableCount;
        private String indexCount;
        private LocalDateTime timestamp;
    }

    public enum IndexRecommendationType {
        CREATE_NEW, DROP_UNUSED, REVIEW_UNDERUTILIZED, OPTIMIZE_EXISTING, REINDEX_BLOATED
    }

    // Inner classes
    @lombok.Data
    public static class QueryAnalysis {
        private String queryName;
        private String sqlQuery;
        private LocalDateTime analysisTimestamp;
        private int complexityScore;
        private boolean hasJoins;
        private boolean hasSubqueries;
        private boolean hasOrderBy;
        private boolean hasGroupBy;
        private boolean hasHaving;
        private int whereClauseCount;
        private List<String> tablesUsed;
        private List<String> potentialIssues;
    }

    @lombok.Data
    @lombok.Builder
    public static class OptimizationRecommendation {
        private String title;
        private String description;
        private String priority; // HIGH, MEDIUM, LOW
        private Map<String, Object> details;
        private LocalDateTime createdAt;
    }
}
