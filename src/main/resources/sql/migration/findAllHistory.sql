SELECT json_agg(h order by h.version)
FROM migration.history h;