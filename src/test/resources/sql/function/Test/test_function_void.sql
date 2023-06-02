create or replace function test_function_void(name text default 'plop') returns void
    language plpgsql
as
$$
begin
    perform 1;
end;
$$;