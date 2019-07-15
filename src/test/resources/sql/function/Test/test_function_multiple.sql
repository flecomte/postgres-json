CREATE OR REPLACE FUNCTION test_function_multiple (name text default 'plop', IN hi text default 'hello', out result json)
LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_array(
        json_build_object('id', 3, 'name', name),
        json_build_object('id', 4, 'name', hi)
    );
END;
$$