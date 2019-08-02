INSERT INTO migration.history as h (filename, executed_at, up, down, version)
VALUES (?, now(), ?, ?, (
    select coalesce(max(version), 0)+1
    from migration.history h2
    where h2.filename = filename
))
RETURNING to_json(h);
