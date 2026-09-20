#!/usr/bin/env bash
# ==============================================================================
# Nexa Platform - Multi-Tenant PostgreSQL Database Backup Script
# ==============================================================================
# Backs up the complete database (public.tenant_registry and all tenant_* schemas)
# using PostgreSQL custom archive format (-F c).
#
# Usage:
#   ./scripts/db-backup.sh [output_file]
#
# Environment Overrides (optional):
#   POSTGRES_USER       Database username (default: saas)
#   POSTGRES_DB         Database name (default: saas_db)
#   POSTGRES_HOST       Database host (default: localhost, or via Docker container)
#   POSTGRES_PORT       Database port (default: 5434)
#   BACKUP_DIR          Directory for backup files (default: ./backups)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
mkdir -p "$BACKUP_DIR"

DB_USER="${POSTGRES_USER:-saas}"
DB_NAME="${POSTGRES_DB:-saas_db}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5434}"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUTPUT_FILE="${1:-$BACKUP_DIR/nexa_${DB_NAME}_${TIMESTAMP}.dump}"

echo "============================================================"
echo "Nexa Database Backup"
echo "============================================================"
echo "Database:  $DB_NAME"
echo "Target:    $OUTPUT_FILE"
echo "Timestamp: $TIMESTAMP"
echo "------------------------------------------------------------"

# Check if Docker compose postgres container is running
USE_DOCKER=false
if command -v docker >/dev/null 2>&1 && docker compose ps postgres 2>/dev/null | grep -q "Up\|running"; then
    USE_DOCKER=true
    echo "Detected running Docker Compose PostgreSQL container. Executing in container..."
fi

if [ "$USE_DOCKER" = true ]; then
    docker compose exec -T postgres pg_dump \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -F c \
        --blobs \
        --verbose > "$OUTPUT_FILE" 2>/dev/null
else
    if ! command -v pg_dump >/dev/null 2>&1; then
        echo "Error: pg_dump client is not installed and Docker container is not active."
        echo "Please start Docker Compose (docker compose up -d postgres) or install PostgreSQL client tools."
        exit 1
    fi
    PGPASSWORD="${POSTGRES_PASSWORD:-saas_dev_pw}" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -F c \
        --blobs \
        --verbose \
        -f "$OUTPUT_FILE"
fi

if [ -s "$OUTPUT_FILE" ]; then
    FILE_SIZE="$(du -h "$OUTPUT_FILE" | cut -f1)"
    echo "------------------------------------------------------------"
    echo "Backup completed successfully!"
    echo "File: $OUTPUT_FILE"
    echo "Size: $FILE_SIZE"
    echo "============================================================"
else
    echo "Error: Backup file is empty or was not created."
    exit 1
fi
