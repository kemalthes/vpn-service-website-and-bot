plugins {
    java
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.springframework.boot") version "4.0.2" apply false
}

group = "io.nesvpn"
version = "0.0.1-SNAPSHOT"
description = "vpn-service"

val springBootVersion = "4.0.2"
val mapstructVersion = "1.6.3"
val caffeineVersion = "3.2.2"
val springdocVersion = "3.0.0"
val telegramBotsVersion = "6.7.0"

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter")
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

configure(listOf(
    project(":db-migrations"),
    project(":rabbitmq-config")
)) {
    apply(plugin = "java-library")
}

project(":db-migrations") {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-liquibase")
    }
}

project(":rabbitmq-config") {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-amqp")
    }
}

configure(listOf(
    project(":backend-site-service"),
    project(":subscribe-link-service"),
    project(":telegram-bot")
)) {
    apply(plugin = "org.springframework.boot")

    tasks.named<Jar>("jar") {
        enabled = false
    }
}

project(":backend-site-service") {
    dependencies {
        implementation(project(":rabbitmq-config"))
        implementation(project(":db-migrations"))
        implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-cache")
        implementation("org.springframework.boot:spring-boot-starter-aop:4.0.0-M1")
        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
        implementation("org.mapstruct:mapstruct:$mapstructVersion")
        implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
        runtimeOnly("org.postgresql:postgresql")
        annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    }
}

project(":subscribe-link-service") {
    dependencies {
        implementation(project(":rabbitmq-config"))
        runtimeOnly("org.postgresql:postgresql")
    }
}

project(":telegram-bot") {
    dependencies {
        implementation(project(":db-migrations"))
        implementation(project(":rabbitmq-config"))
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.telegram:telegrambots-spring-boot-starter:6.7.0")
        runtimeOnly("org.postgresql:postgresql")
        implementation("org.telegram:telegrambots-spring-boot-starter:$telegramBotsVersion")
        implementation("io.github.neodix42:tonlib:0.5.0")
        implementation("com.google.zxing:core:3.5.3")
        implementation("com.google.zxing:javase:3.5.3")
        implementation("org.telegram:telegrambots:6.9.7.1")
        implementation("commons-codec:commons-codec:1.16.0")
        implementation("org.slf4j:slf4j-api:2.0.9")
    }
}
