#!/usr/bin/env bash
# ==============================================================================
# Nexa Platform - Multi-Tenant PostgreSQL Database Restore Script
# ==============================================================================
# Restores a PostgreSQL custom archive dump (-F c) into the database.
#
# SAFETY NOTICE:
#   By default, this script will NOT perform destructive operations without
#   explicit user confirmation (--confirm flag or interactive 'YES' prompt).
#
# Usage:
#   ./scripts/db-restore.sh <path_to_dump_file> [--confirm]
#
# Environment Overrides (optional):
#   POSTGRES_USER       Database username (default: saas)
#   POSTGRES_DB         Database name (default: saas_db)
#   POSTGRES_HOST       Database host (default: localhost, or via Docker container)
#   POSTGRES_PORT       Database port (default: 5434)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <path_to_dump_file> [--confirm]"
    exit 1
fi

BACKUP_FILE="$1"
CONFIRM_FLAG="${2:-}"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file does not exist: $BACKUP_FILE"
    exit 1
fi

DB_USER="${POSTGRES_USER:-saas}"
DB_NAME="${POSTGRES_DB:-saas_db}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5434}"

echo "============================================================"
echo "Nexa Database Restore"
echo "============================================================"
echo "Backup File: $BACKUP_FILE"
echo "Target DB:   $DB_NAME"
echo "Target User: $DB_USER"
echo "------------------------------------------------------------"
echo "WARNING: Restoring will overwrite existing data in schemas"
echo "matching the dump (public.tenant_registry and tenant schemas)."
echo "------------------------------------------------------------"

if [ "$CONFIRM_FLAG" != "--confirm" ]; then
    read -r -p "Are you sure you want to proceed with restore? Type 'YES' to continue: " USER_INPUT
    if [ "$USER_INPUT" != "YES" ]; then
        echo "Restore cancelled by user. No changes were made."
        exit 0
    fi
fi

USE_DOCKER=false
if command -v docker >/dev/null 2>&1 && docker compose ps postgres 2>/dev/null | grep -q "Up\|running"; then
    USE_DOCKER=true
    echo "Detected running Docker Compose PostgreSQL container. Executing in container..."
fi

echo "Restoring database from $BACKUP_FILE..."

if [ "$USE_DOCKER" = true ]; then
    docker compose exec -T postgres pg_restore \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --clean \
        --if-exists \
        --no-owner \
        --no-privileges \
        --verbose < "$BACKUP_FILE" || {
            STATUS=$?
            # pg_restore returns 1 if warnings were issued during restore (e.g. non-fatal table drops)
            if [ $STATUS -eq 1 ]; then
                echo "Notice: pg_restore exited with code 1 (warnings issued, typically non-fatal drop notices)."
            else
                echo "Error: pg_restore failed with exit code $STATUS"
                exit $STATUS
            fi
        }
else
    if ! command -v pg_restore >/dev/null 2>&1; then
        echo "Error: pg_restore client is not installed and Docker container is not active."
        exit 1
    fi
    PGPASSWORD="${POSTGRES_PASSWORD:-saas_dev_pw}" pg_restore \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --clean \
        --if-exists \
        --no-owner \
        --no-privileges \
        --verbose \
        "$BACKUP_FILE" || {
            STATUS=$?
            if [ $STATUS -eq 1 ]; then
                echo "Notice: pg_restore exited with code 1 (warnings issued, typically non-fatal drop notices)."
            else
                echo "Error: pg_restore failed with exit code $STATUS"
                exit $STATUS
            fi
        }
fi

echo "------------------------------------------------------------"
echo "Database restore completed successfully."
echo "============================================================"
