#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 --username "test" --dbname "json_test" <<-EOSQL
create extension if not exists plpgsql;
create extension if not exists "uuid-ossp";
EOSQL
