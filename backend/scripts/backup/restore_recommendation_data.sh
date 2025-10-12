#!/bin/bash
# Restore script for recommendation engine data
# Usage: ./restore_recommendation_data.sh <backup_file.sql.gz>

set -e  # Exit on error

# Check arguments
if [ -z "$1" ]; then
    echo "Error: Backup file not specified"
    echo "Usage: $0 <backup_file.sql.gz>"
    echo ""
    echo "Example:"
    echo "  $0 /var/backups/youthmind/recommendation/recommendation_data_20241012_143000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

# Check if backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-youthmind}"
DB_USER="${DB_USER:-postgres}"
TEMP_SQL="/tmp/recommendation_restore_$$.sql"

echo "========================================="
echo "YouthMind Recommendation Engine Restore"
echo "========================================="
echo "Backup File: $BACKUP_FILE"
echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
echo ""

# Warning prompt
read -p "This will restore data to $DB_NAME. Continue? (yes/no): " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Restore cancelled."
    exit 0
fi

# Decompress backup
echo "Decompressing backup..."
gunzip -c "$BACKUP_FILE" > "$TEMP_SQL"

# Restore data
echo "Restoring data..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$TEMP_SQL"

# Cleanup temp file
rm -f "$TEMP_SQL"

echo ""
echo "Restore complete!"
echo ""

# Display row counts
echo "Verifying restored data:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT 
    (SELECT COUNT(*) FROM user_recommendation_vectors) as r_vectors,
    (SELECT COUNT(*) FROM recommendation_mapping_rules) as mapping_rules,
    (SELECT COUNT(*) FROM user_feed_state) as feed_states,
    (SELECT COUNT(*) FROM feed_block_cache) as feed_blocks;
"

exit 0

