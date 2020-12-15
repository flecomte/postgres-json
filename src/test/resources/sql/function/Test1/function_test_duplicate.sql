CREATE OR REPLACE FUNCTION test_function_duplicate (name text default 'plop') returns json
    LANGUAGE plpgsql
AS
$$
BEGIN
    return json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name);
END;
$$
