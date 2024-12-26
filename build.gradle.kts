plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.datadoghq.pej"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.opentelemetry:opentelemetry-api")

}

dependencyManagement {
    imports {
        mavenBom("io.opentelemetry:opentelemetry-bom:1.45.0")
    }
}

