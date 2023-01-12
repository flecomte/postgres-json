CREATE OR REPLACE FUNCTION function_multiparam (
    name varchar(45) default 'plop',
    numeric(4, 5),
    num float(5),
    num2 timestamp without time zone default '2002-01-01T00:00:00'::timestamp,
    num3 int,
    num4 integer,
    num5 smallint,
    num6 bigint,
    num7 decimal,
    num8 decimal(4, 6),
    num9 real,
    num10 double precision,
    num11 smallserial,
    num12 serial,
    "num13" bigserial,
    num14 serial,
    num15 money,
    num16 character varying(789),
    num16b character varying(789) default 'abc',
    num16c character varying default 'abc',
    num17 character(56),
    num18 char(2),
    num19 any,
    num20 anyelement,
    num21 anyarray
)
LANGUAGE plpgsql
AS
$$
BEGIN
    PERFORM 1;
END;
$$;
