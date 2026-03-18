FROM postgres:16-alpine

COPY migrations/ /docker-entrypoint-initdb.d/migrations/
COPY flyway.conf /flyway/conf/flyway.conf

# Flyway for migrations
RUN apk add --no-cache curl bash \
    && curl -sL https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/10.6.0/flyway-commandline-10.6.0-linux-x64.tar.gz | tar xz -C /opt \
    && ln -s /opt/flyway-10.6.0/flyway /usr/local/bin/flyway

HEALTHCHECK --interval=5s --timeout=5s --retries=5 CMD pg_isready -U postgres
