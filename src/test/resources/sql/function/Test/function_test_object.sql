CREATE OR REPLACE FUNCTION test_function_object (inout resource json)
LANGUAGE plpgsql
AS
$$
BEGIN
    resource = json_build_object('id', 1, 'name', 'changedName');
END;
$$