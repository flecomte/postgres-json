INSERT INTO migration.history as h (filename, executed_at, up, down, version)
VALUES (?, now(), ?, ?, nextval('migration.version_seq'))
RETURNING to_json(h);
