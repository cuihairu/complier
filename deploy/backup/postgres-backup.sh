#!/bin/bash
# PostgreSQL Backup Script for Oddsmaker
# This script performs automated backups of the PostgreSQL database

set -e

# Configuration
BACKUP_DIR="/backup/postgres"
RETENTION_DAYS=30
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-oddsmaker}"
DB_USER="${DB_USER:-oddsmaker}"
BACKUP_TYPE="${1:-full}"  # full or incremental

# Logging
LOG_FILE="/var/log/postgres-backup.log"
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

# Generate backup filename
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/${BACKUP_TYPE}_${TIMESTAMP}.sql.gz"

# Start backup
log "Starting $BACKUP_TYPE backup of $DB_NAME"

if [ "$BACKUP_TYPE" = "full" ]; then
    # Full backup using pg_dump
    pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --format=custom \
        --compress=9 \
        --verbose \
        --file="$BACKUP_FILE" 2>> "$LOG_FILE"

    log "Full backup completed: $BACKUP_FILE"
else
    # Incremental backup (schema-only + recent data)
    SCHEMA_FILE="$BACKUP_DIR/schema_${TIMESTAMP}.sql"
    DATA_FILE="$BACKUP_DIR/data_${TIMESTAMP}.sql"

    # Backup schema
    pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --schema-only \
        --file="$SCHEMA_FILE" 2>> "$LOG_FILE"

    # Backup recent data (last 7 days)
    pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --data-only \
        --where="created_at > NOW() - INTERVAL '7 days'" \
        --file="$DATA_FILE" 2>> "$LOG_FILE"

    # Compress
    tar -czf "$BACKUP_FILE" "$SCHEMA_FILE" "$DATA_FILE"
    rm -f "$SCHEMA_FILE" "$DATA_FILE"

    log "Incremental backup completed: $BACKUP_FILE"
fi

# Calculate backup size
BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
log "Backup size: $BACKUP_SIZE"

# Verify backup integrity
log "Verifying backup integrity"
pg_restore --list "$BACKUP_FILE" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    log "Backup verification successful"
else
    log "ERROR: Backup verification failed"
    exit 1
fi

# Upload to remote storage (if configured)
if [ -n "$S3_BUCKET" ]; then
    log "Uploading backup to S3: $S3_BUCKET"
    aws s3 cp "$BACKUP_FILE" "s3://$S3_BUCKET/postgres/" --storage-class STANDARD_IA
    log "S3 upload completed"
fi

# Clean up old backups
log "Cleaning up backups older than $RETENTION_DAYS days"
find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete
log "Cleanup completed"

# Calculate total backup size
TOTAL_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)
log "Total backup storage: $TOTAL_SIZE"

# Send notification (if configured)
if [ -n "$WEBHOOK_URL" ]; then
    curl -s -X POST "$WEBHOOK_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"text\": \"✅ PostgreSQL backup completed\",
            \"details\": {
                \"type\": \"$BACKUP_TYPE\",
                \"file\": \"$BACKUP_FILE\",
                \"size\": \"$BACKUP_SIZE\",
                \"database\": \"$DB_NAME\"
            }
        }"
fi

log "Backup process completed successfully"
