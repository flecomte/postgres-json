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
$$