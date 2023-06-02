create or replace function test_function_object(inout resource json)
    language plpgsql
as
$$
begin
    resource = json_build_object('id', '1e5f5d41-6d14-4007-897b-0ed2616bec96', 'name', 'changedName');
end;
$$