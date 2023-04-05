-- create database test;
-- create user test with encrypted password 'test';
-- grant all privileges on database test to test;
-- ALTER SCHEMA public owner to test;

drop schema IF EXISTS public cascade;
create schema if not exists public;

create table if not exists test
(
    id uuid not null
        constraint test_pk
            primary key,
    name text
);

create table if not exists test2
(
    id uuid not null,
    title text,
    test_id uuid
        constraint test2_test_id_fk
            references test
);

INSERT INTO test (id, name) VALUES ('1e5f5d41-6d14-4007-897b-0ed2616bec96', 'plop') ON CONFLICT DO NOTHING;
INSERT INTO test2 (id, title, test_id) VALUES ('1e5f5d41-6d14-4007-897b-0ed2616bec96', 'plop', '1e5f5d41-6d14-4007-897b-0ed2616bec96') ON CONFLICT DO NOTHING;
INSERT INTO test2 (id, title, test_id) VALUES ('829b1a29-5db8-47f9-9562-961c561ac528', 'plip', '1e5f5d41-6d14-4007-897b-0ed2616bec96') ON CONFLICT DO NOTHING;
INSERT INTO test2 (id, title, test_id) VALUES ('457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'ttt', null) ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION test_function (name text default 'plop', IN hi text default 'hello', out result json)
    LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name);
END;
$$;


CREATE OR REPLACE FUNCTION test_function_multiple (name text default 'plop', IN hi text default 'hello', out result json)
    LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_array(
            json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name),
            json_build_object('id', '8d20abb0-7f77-4b6c-9991-44acd3c88faa', 'name', hi)
        );
END;
$$;

CREATE OR REPLACE FUNCTION test_function_paginated (name text default 'plop', IN "limit" int default 10, IN "offset" int default 0, out result json, out total int)
    LANGUAGE plpgsql
AS
$$
BEGIN
    SELECT json_build_array(
                   json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name::text),
                   json_build_object('id', '8d20abb0-7f77-4b6c-9991-44acd3c88faa', 'name', name::text || '-2')
               ),
           10
    INTO result, total
    LIMIT "limit" OFFSET "offset";
END;
$$;

CREATE OR REPLACE FUNCTION test_function_object (inout resource json)
    LANGUAGE plpgsql
AS
$$
BEGIN
    resource = json_build_object('id', '1e5f5d41-6d14-4007-897b-0ed2616bec96', 'name', 'changedName');
END;
$$;

CREATE OR REPLACE FUNCTION test_function_void (name text default 'plop') returns void
    LANGUAGE plpgsql
AS
$$
BEGIN
    PERFORM 1;
END;
$$