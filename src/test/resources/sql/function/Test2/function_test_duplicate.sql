CREATE OR REPLACE FUNCTION test_function_duplicate (name text default 'plop') returns text
    LANGUAGE plpgsql
AS
$$
BEGIN
    return name;
END;
$$