CREATE OR REPLACE FUNCTION function_void (name text default 'plop') returns void
LANGUAGE plpgsql
AS
$$
BEGIN
    PERFORM 1;
END;
$$;