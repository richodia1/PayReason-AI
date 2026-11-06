# Infrastructure

The root `compose.yaml` owns the local deployment. All published ports bind to 127.0.0.1 and the PostgreSQL volume is isolated under the `payreason` Compose project.

Default ports: console 5180, Java API 8180, Python API 8181, PostgreSQL 5544. RabbitMQ is optional with `docker compose --profile events up -d rabbitmq`; no application consumer is connected yet.

Do not run `docker compose down -v` unless you deliberately want to delete local data. Cloud deployment is a later milestone and is not configured or published by this scaffold.
