create or replace function test_function_paginated(
    name text default 'plop',
    in "limit" int default 10,
    in "offset" int default 0,
    out result json,
    out total int
)
    language plpgsql
as
$$
begin
    select
        json_build_array(
            json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name::text),
            json_build_object('id', '8d20abb0-7f77-4b6c-9991-44acd3c88faa', 'name', name::text || '-2')
        ),
        10
    into result, total
    limit "limit" offset "offset";
end;
$$