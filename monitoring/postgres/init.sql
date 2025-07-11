-- Enable pg_stat_statements extension for query monitoring
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
 
-- Reset any existing stats
SELECT pg_stat_statements_reset(); 