CREATE OR REPLACE FUNCTION test_function_duplicate (name text default 'plop') returns json
    LANGUAGE plpgsql
AS
$$
BEGIN
    return json_build_object('id', 3, 'name', name);
END;
$$
