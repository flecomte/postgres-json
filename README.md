PostgresJson
============
_Kotlin library to request postgres with native SQL queries_

[![Tests](https://github.com/flecomte/postgres-json/actions/workflows/gradle.yml/badge.svg)](https://github.com/flecomte/postgres-json/actions/workflows/gradle.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=postgres-json&metric=coverage)](https://sonarcloud.io/dashboard?id=postgres-json)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=postgres-json&metric=alert_status)](https://sonarcloud.io/dashboard?id=postgres-json)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=postgres-json&metric=ncloc)](https://sonarcloud.io/dashboard?id=postgres-json)

---

## What is this lib for?
This library allows you to make sql requests and return the result in json format, then deserialize it into an entity.
It also allows you to save an entity (INSERT) by serializing it and sending the json to the database, allowing you to insert several entities with their children, in a single request.

It also manages the migrations of the schema of tables and stored procedures.

All sql requests are handled manually for full control over what you do.

---

## The best benefits

* Total control of all Postgresql features and SQL language
* More speed and flexible than an ORM
* [Multi level request](./docs/usage/multi-level.md) (Can return multiple tables and these children in a single request)
* Queries are written in separate native `.sql` files
* Unit testing of SQL queries
* Migrations are written in separate native `.sql` files
* Automatic tested database migration and rollback
---
## Documentation: Table of Contents

* [Installation](./docs/installation.md)
* [Migrations](./docs/migrations/migrations.md)
* [Usage](./docs/usage/usage.md)
* [How that works](./docs/call%20function.png) (Diagram)
* [How to begin](./docs/checklist.md)
