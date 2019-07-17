CREATE OR REPLACE FUNCTION test_function_paginated (name text default 'plop', IN "limit" int default 10, IN "offset" int default 0, out result json, out total int)
LANGUAGE plpgsql
AS
$$
BEGIN
    SELECT json_build_array(
        json_build_object('id', 3, 'name', name::text),
        json_build_object('id', 4, 'name', name::text || '-2')
    ),
    10
    INTO result, total
    LIMIT "limit" OFFSET "offset";
END;
$$