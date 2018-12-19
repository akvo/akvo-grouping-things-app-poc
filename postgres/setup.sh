#!/usr/bin/env bash

set -e

psql -c "CREATE ROLE lumen WITH PASSWORD 'lumenpasswd' CREATEDB LOGIN;"

psql -c "CREATE DATABASE lumen WITH OWNER = lumen TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8';"

psql -d lumen -c "CREATE EXTENSION IF NOT EXISTS ltree WITH SCHEMA public;"
psql -d lumen -c "CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;"

