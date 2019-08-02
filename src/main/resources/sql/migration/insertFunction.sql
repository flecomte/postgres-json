INSERT INTO migration.functions as f (filename, definition, executed_at, up, down, version)
VALUES (?, ?, now(), ?, ?, (
    select coalesce(max(version), 0)+1
    from migration.functions f2
    where filename = f2.filename
))
RETURNING to_json(f);