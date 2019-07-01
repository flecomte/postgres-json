-- create database test;
-- create user test with encrypted password 'test';
-- grant all privileges on database test to test;
-- ALTER SCHEMA public owner to test;

drop schema IF EXISTS public cascade;
create schema if not exists public;

create table if not exists test
(
    id serial not null
        constraint test_pk
            primary key,
    name text
);

create table if not exists test2
(
    id serial not null,
    title text,
    test_id integer
        constraint test2_test_id_fk
            references test
);

INSERT INTO test (id, name) VALUES (1, 'plop') ON CONFLICT DO NOTHING;
INSERT INTO test2 (id, title, test_id) VALUES (1, 'plop', 1) ON CONFLICT DO NOTHING;
INSERT INTO test2 (id, title, test_id) VALUES (2, 'plip', 1) ON CONFLICT DO NOTHING;
INSERT INTO test2 (id, title, test_id) VALUES (3, 'ttt', null) ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION test_function (name text default 'plop', IN hi text default 'hello', out result json)
    LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_object('id', 3, 'name', 'test');
END;
$$
