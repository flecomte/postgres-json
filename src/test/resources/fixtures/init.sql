create table if not exists test
(
    id serial not null
        constraint test_pk
            primary key,
    name text
);

create table if not exists test2
(
    id serial not null,
    title text,
    test_id integer
        constraint test2_test_id_fk
            references test
);
INSERT INTO public.test (id, name) VALUES (1, 'plop');
INSERT INTO public.test2 (id, title, test_id) VALUES (1, 'plop', 1);
INSERT INTO public.test2 (id, title, test_id) VALUES (2, 'plip', 1);
INSERT INTO public.test2 (id, title, test_id) VALUES (3, 'ttt', null);