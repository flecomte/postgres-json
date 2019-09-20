CREATE OR REPLACE FUNCTION test_function_duplicate (name text default 'plop', out result text)
    LANGUAGE plpgsql
AS
$$
BEGIN
    result = name;
END;
$$