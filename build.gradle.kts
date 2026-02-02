plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.nesvpn"
version = "0.0.1-SNAPSHOT"
description = "vpn-service"

allprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
    }
}

subprojects {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter")
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }
}

project(":db-migrations") {
    apply(plugin = "java-library")

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-liquibase")
    }
}

project(":rabbitmq-config") {
    apply(plugin = "java-library")

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-amqp")
    }
}

project(":backend-site-service") {
    apply(plugin = "org.springframework.boot")

    dependencies {
        implementation(project(":rabbitmq-config"))
        implementation(project(":db-migrations"))
        implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-webmvc")
        implementation("org.springframework.boot:spring-boot-starter-cache")
        implementation("org.springframework.boot:spring-boot-starter-aop:4.0.0-M1")
        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")
        implementation("org.mapstruct:mapstruct:1.6.3")
        implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")
        runtimeOnly("org.postgresql:postgresql")
        annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    }

    tasks.named<Jar>("jar") {
        enabled = false
    }
}

project(":subscribe-link-service") {
    apply(plugin = "org.springframework.boot")

    dependencies {
        implementation(project(":rabbitmq-config"))

        implementation("org.postgresql:postgresql")
    }

    tasks.named<Jar>("jar") {
        enabled = false
    }
}

project(":telegram-bot") {
    apply(plugin = "org.springframework.boot")

    dependencies {
        implementation(project(":rabbitmq-config"))
        implementation(project(":db-migrations"))
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.telegram:telegrambots-spring-boot-starter:6.7.0")
    }

    tasks.named<Jar>("jar") {
        enabled = false
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
