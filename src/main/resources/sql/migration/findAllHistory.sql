SELECT json_object_agg(filename, f)
FROM migration.functions f;