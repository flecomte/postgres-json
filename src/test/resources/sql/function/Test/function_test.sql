CREATE OR REPLACE FUNCTION test_function (name text default 'plop', IN hi text default 'hello', out result json)
LANGUAGE plpgsql
AS
$$
BEGIN
    result = json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name);
END;
$$