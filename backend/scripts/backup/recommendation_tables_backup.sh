#!/bin/bash
# Backup script for recommendation engine tables
# Usage: ./recommendation_tables_backup.sh

set -e  # Exit on error

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/var/backups/youthmind/recommendation}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-youthmind}"
DB_USER="${DB_USER:-postgres}"

# Create backup directory
mkdir -p "$BACKUP_DIR"

echo "========================================="
echo "YouthMind Recommendation Engine Backup"
echo "========================================="
echo "Timestamp: $TIMESTAMP"
echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
echo "Backup Directory: $BACKUP_DIR"
echo ""

# Backup data from critical recommendation tables
echo "Backing up recommendation data..."
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --table=user_recommendation_vectors \
  --table=recommendation_mapping_rules \
  --table=user_feed_state \
  --table=feed_block_cache \
  --data-only \
  --file="$BACKUP_DIR/recommendation_data_$TIMESTAMP.sql"

# Backup schema (for reference and recovery)
echo "Backing up recommendation schema..."
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --table=user_recommendation_vectors \
  --table=recommendation_mapping_rules \
  --table=user_feed_state \
  --table=feed_block_cache \
  --schema-only \
  --file="$BACKUP_DIR/recommendation_schema_$TIMESTAMP.sql"

# Compress backups
echo "Compressing backups..."
gzip "$BACKUP_DIR/recommendation_data_$TIMESTAMP.sql"
gzip "$BACKUP_DIR/recommendation_schema_$TIMESTAMP.sql"

# Calculate sizes
DATA_SIZE=$(du -h "$BACKUP_DIR/recommendation_data_$TIMESTAMP.sql.gz" | cut -f1)
SCHEMA_SIZE=$(du -h "$BACKUP_DIR/recommendation_schema_$TIMESTAMP.sql.gz" | cut -f1)

echo ""
echo "Backup complete!"
echo "  Data backup: $BACKUP_DIR/recommendation_data_$TIMESTAMP.sql.gz ($DATA_SIZE)"
echo "  Schema backup: $BACKUP_DIR/recommendation_schema_$TIMESTAMP.sql.gz ($SCHEMA_SIZE)"
echo ""

# Cleanup old backups (keep last 30 days)
echo "Cleaning up backups older than 30 days..."
find "$BACKUP_DIR" -name "recommendation_*.sql.gz" -mtime +30 -delete
echo "Cleanup complete!"

# Display current backups
echo ""
echo "Current backups:"
ls -lh "$BACKUP_DIR"/recommendation_*.sql.gz | tail -5

exit 0

