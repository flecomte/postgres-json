SELECT json_agg(f order by f.version)
FROM migration.functions f;