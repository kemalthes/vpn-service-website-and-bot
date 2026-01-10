# Впн сервис

---

## Информация для бэкенд-разработчика

Дамп базы данных в запущенном контейнере:

`docker compose exec postgres pg_dump -U postgres --clean --if-exists --no-owner vpn_db > dev_dump.sql`