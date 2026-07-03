plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "no.iktdev"
version = "0.0.1-SNAPSHOT"
description = "Kammich"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://reposilite.iktdev.no/releases") }
	maven { url = uri("https://reposilite.iktdev.no/snapshots") }

}

val exposedVersion = "1.3.1"


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")

	implementation("no.iktdev:exfl:1.0-rc1")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
	implementation("com.google.code.gson:gson:2.8.9")
	implementation("org.json:json:20231013")

	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-nc-sqlite:12.10.0")
	implementation("org.xerial:sqlite-jdbc:3.45.1.0")
	// SLF4J for logging (Exposed og Flyway trenger en logger)

	// Exposed
	implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")


	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
