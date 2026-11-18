#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    CREATE USER keycloak WITH PASSWORD '${KEYCLOAK_DB_PASSWORD}';
    CREATE DATABASE keycloak_db OWNER keycloak;

    CREATE USER library WITH PASSWORD '${LIBRARY_DB_PASSWORD}';
    CREATE DATABASE library_db OWNER library;
EOSQL