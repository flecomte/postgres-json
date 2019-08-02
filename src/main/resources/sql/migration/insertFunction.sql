INSERT INTO migration.functions as f (filename, definition, executed_at, up, down, version)
VALUES (?, ?, now(), ?, ?, (
    select coalesce(max(version), 0)+1
    from migration.functions f2
    where filename = f2.filename
))
ON CONFLICT (filename) DO UPDATE SET
    definition = excluded.definition,
    up = excluded.up,
    down = excluded.down,
    version = excluded.version,
    executed_at = now()
RETURNING to_json(f);