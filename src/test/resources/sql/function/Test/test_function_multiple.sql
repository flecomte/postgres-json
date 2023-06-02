create or replace function test_function_multiple(name text default 'plop', in hi text default 'hello', out result json)
    language plpgsql
as
$$
begin
    result = json_build_array(
            json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name),
            json_build_object('id', '8d20abb0-7f77-4b6c-9991-44acd3c88faa', 'name', hi)
        );
end;
$$