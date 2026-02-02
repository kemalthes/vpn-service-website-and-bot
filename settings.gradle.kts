rootProject.name = "vpn-service"

include("db-migrations")
include("rabbitmq-config")
include("subscribe-link-service")
include("telegram-bot")
include("backend-site-service")

project(":db-migrations").projectDir = file("shared/db-migrations")
project(":rabbitmq-config").projectDir = file("shared/rabbitmq-config")
project(":subscribe-link-service").projectDir = file("services/subscribe-link-service")
project(":telegram-bot").projectDir = file("services/telegram-bot")
project(":backend-site-service").projectDir = file("services/backend-site-service")