package ai.youthmind.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement
public class DatabasePerformanceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        // Connection pool settings for high performance
        config.setMaximumPoolSize(50); // Maximum connections in pool
        config.setMinimumIdle(10); // Minimum idle connections
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        // Performance optimizations
        config.setCachePrepStmts(true);
        config.setPrepStmtCacheSize(250);
        config.setPrepStmtCacheSqlLimit(2048);
        config.setUseServerPrepStmts(true);
        config.setUseLocalSessionState(true);
        config.setRewriteBatchedStatements(true);
        config.setCacheResultSetMetadata(true);
        config.setCacheServerConfiguration(true);
        config.setElideSetAutoCommits(true);
        config.setMaintainTimeStats(false);
        
        // Pool name for monitoring
        config.setPoolName("YouthMind-HikariCP");
        
        // Health check query
        config.setConnectionTestQuery("SELECT 1");
        
        // Additional PostgreSQL specific optimizations
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "10");
        config.addDataSourceProperty("prepareThreshold", "1");
        config.addDataSourceProperty("preparedStatementCacheQueries", "256");
        config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        config.addDataSourceProperty("databaseMetadataCacheFields", "65536");
        config.addDataSourceProperty("databaseMetadataCacheFieldsMiB", "5");
        config.addDataSourceProperty("stringtype", "unspecified");
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
