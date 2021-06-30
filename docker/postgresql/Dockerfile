FROM postgres:13

COPY postgresql.conf /tmp/postgresql.conf
COPY extension.sh    /docker-entrypoint-initdb.d/000-extension.sh
COPY setup.sh        /docker-entrypoint-initdb.d/100-setup.sh

ENTRYPOINT ["docker-entrypoint.sh"]
EXPOSE 5432
CMD ["postgres"]
