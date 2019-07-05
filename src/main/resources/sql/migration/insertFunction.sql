INSERT INTO migration.functions as f (filename, definition, executed_at, up, down, version)
VALUES (?, ?, now(), ?, ?, ?)
RETURNING to_json(f);