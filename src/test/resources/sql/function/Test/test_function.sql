create or replace function test_function(name text default 'plop', in hi text default 'hello', out result json)
    language plpgsql
as
$$
begin
    result = json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name);
end;
$$