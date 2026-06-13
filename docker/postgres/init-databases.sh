#!/bin/sh
set -eu

create_service_database() {
  database_name="$1"
  role_name="$2"
  role_password="$3"

  psql --set=ON_ERROR_STOP=1 \
    --set=role_name="$role_name" \
    --set=role_password="$role_password" \
    --set=database_name="$database_name" \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" <<-'SQL'
SELECT format(
  'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
  :'role_name',
  :'role_password'
) \gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'database_name', :'role_name') \gexec
SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'database_name') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database_name', :'role_name') \gexec
SQL
}

psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-'SQL'
REVOKE CONNECT ON DATABASE postgres FROM PUBLIC;
REVOKE CONNECT ON DATABASE template1 FROM PUBLIC;
SQL

create_service_database auth_db auth_user "$AUTH_DB_PASSWORD"
create_service_database marketplace_db marketplace_user "$MARKETPLACE_DB_PASSWORD"
create_service_database crm_db crm_user "$CRM_DB_PASSWORD"
create_service_database notification_db notification_user "$NOTIFICATION_DB_PASSWORD"
create_service_database admin_db admin_user "$ADMIN_DB_PASSWORD"
create_service_database subscription_db subscription_user "$SUBSCRIPTION_DB_PASSWORD"

touch "$PGDATA/.tutorspace-init-complete"
