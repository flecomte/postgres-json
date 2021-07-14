Multi Level Queries
===================

## Define schema, query and kotlin object
1. Schema
```postgresql
create table parent (
    id uuid primary key,
    name text not null
);

create table child (
    id uuid primary key,
    name text not null,
    parent_id uuid not null references parent
)
```
2. Insert some data for tests
```postgresql
insert into parent (id, name) VALUES ('379e0687-9e4a-4781-b0e9-d94a62e4261f', 'Bernard');
insert into child (id, name, parent_id) VALUES (uuid_generate_v4(), 'Noé', '379e0687-9e4a-4781-b0e9-d94a62e4261f');
insert into child (id, name, parent_id) VALUES (uuid_generate_v4(), 'John', '379e0687-9e4a-4781-b0e9-d94a62e4261f');
```
3. Define Model
```kotlin
import java.util.UUID

class Parent(val id: UUID, val name: String, val children: List<Child>)
class Child(val id: UUID, val name: String)
```

4. Define request function
```postgresql
-- resource/sql/functions/find_parent_by_id.sql
create or replace function find_parent_by_id(in _id uuid, out resource json) language plpgsql as
$$
begin
    select to_json(t) into resource
    from (
        select 
            p.*,
            json_agg(to_jsonb(c) - 'parent_id') as children   
        from parent p
        join child c on c.parent_id = p.id
        where p.id = _id
        group by p.id
    ) t;
end;
$$;
```

## Execute the function

You just to use `Requester` and set the sql function name, then pass arguments.

If you need to return more than one entry, use `.select()` instead of `.selecteOne()`

See the [Paginated example](./paginated.md)
```kotlin
import fr.postgresjson.connexion.Requester

val requester: Requester = TODO()
val result: Parent = requester
    .getFunction("find_parent_by_id")
    .selectOne("id" to "379e0687-9e4a-4781-b0e9-d94a62e4261f")
```

The requester create dynamically this request
```postgresql
select * from find_parent_by_id(_id => '379e0687-9e4a-4781-b0e9-d94a62e4261f');
```
*Watch the underscore as prefix is added if necessary.
The requester known the parameters because it parses all SQL functions and reads the names of the parameters from them.*


And the SQL return is a JSON like follow:
```json
{
  "id": "379e0687-9e4a-4781-b0e9-d94a62e4261f",
  "name": "Bernard",
  "child": [
    {
      "id": "c2d0ec81-7cac-4689-8086-2644a3b309b5",
      "name": "Noé"
    },
    {
      "id": "255d911a-0cbc-4156-bf8c-0204e89494d9",
      "name": "John"
    }
  ]
}
```
But the requester deserialize the result automatically into a Kotlin object with their children objects. **And do that in only one request**.
