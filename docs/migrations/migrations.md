# Migration
## Schemas migration
Migrations are just manually written `*.sql` files that represent the database schemas.
Each file is executed one after the other in alphabetical order.
Each execution is stored in the `migration.history` table.

A migration contains a `*.up.sql` file and a `*.down.sql` file to rollback the migration.
The content of the `*.down.sql` file is also stored in the database.
This allows the `*.down.sql` to be executed even if the code is already rollback.

Example:
```postgresql
-- resources/sql/migrations/0000-init_schema.up.sql
create table "user"
(
    id         uuid        default uuid_generate_v4() not null primary key,
    created_at timestamptz default now()              not null,
    blocked_at timestamptz default null               null,
    username   varchar(64)                            not null check ( username != '' and lower(username) = username) unique,
    password   text                                   not null check ( password != '' ),
    roles      text[]      default '{}'               not null
);
```

```postgresql
-- resources/sql/migrations/0000-init_schema.down.sql
drop table if exists "user";
```
## Stored procedure migrations

Migrations are also stored procedures and other functions.
Each function is updated with each migration.

Example:
```postgresql
-- resources/sql/functions/insert_user.sql
create or replace function insert_user(inout resource json) language plpgsql as
$$
declare
    new_id uuid;
begin
    insert into "user" (id, username, password, blocked_at, roles)
    select
        coalesce(t.id, uuid_generate_v4()),
        t.username,
        crypt(resource->>'password', gen_salt('bf', 8)),
        case when t.blocked_at is not null then now() else null end,
        t.roles
    from json_populate_record(null::"user", resource) t
    returning id into new_id;

    select find_user_by_id(new_id) into resource;
end;
$$;
```

```postgresql
-- resources/sql/functions/find_user_by_id.sql
create or replace function find_user_by_id(in _id uuid, out resource json) language plpgsql as
$$
begin
    select to_jsonb(u) - 'password' into resource
    from "user" as u
    where u.id = _id;
end;
$$;
```

* [Execute migrations in application](./migrations-application.md)
* [Execute migrations with gradle](./migrations-gradle.md)

