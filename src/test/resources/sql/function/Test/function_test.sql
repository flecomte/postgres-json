CREATE OR REPLACE FUNCTION test_function (name text, IN hi text default 'hello', out result json)
LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_object('id', 2, 'name', 'test');
END;
$$