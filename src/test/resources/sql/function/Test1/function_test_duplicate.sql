CREATE OR REPLACE FUNCTION test_function_duplicate (name text default 'plop', out result json)
LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_object('id', 3, 'name', name);
END;
$$