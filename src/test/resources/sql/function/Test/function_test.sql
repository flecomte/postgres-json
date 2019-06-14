CREATE OR REPLACE FUNCTION test_function (name text default 'plop', IN hi text default 'hello', out result json)
LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_object('id', 3, 'name', 'test');
END;
$$