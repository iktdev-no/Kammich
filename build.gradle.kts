import cz.habarta.typescript.generator.JsonLibrary
import cz.habarta.typescript.generator.TypeScriptOutputKind

plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("cz.habarta.typescript-generator") version "4.1.1"
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
val flywayVersion = "12.4.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")

	implementation("no.iktdev:exfl:1.0-rc1")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
	implementation("com.google.code.gson:gson:2.8.9")
	implementation("org.json:json:20231013")

	implementation("org.flywaydb:flyway-core:${flywayVersion}")
	implementation("org.flywaydb:flyway-database-nc-sqlite:${flywayVersion}")
	implementation("org.xerial:sqlite-jdbc:3.45.1.0")
	implementation("com.zaxxer:HikariCP:7.0.2")

	// SLF4J for logging (Exposed og Flyway trenger en logger)

	// Exposed
	implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")


	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("io.mockk:mockk:1.13.9")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks {
	generateTypeScript {
		// Hvor TypeScript-filen skal havne
		outputFile = "$projectDir/web/src/types/types.d.ts"

		// Hva slags TS-output du vil ha
		outputKind = TypeScriptOutputKind.module

		// Hvilket JSON-bibliotek Kotlin bruker
		jsonLibrary = JsonLibrary.jackson2

		// Valgfritt: litt bedre defaults
		classPatterns = listOf("no.iktdev.kammich.models.shared.**")
	}
}

tasks.named("generateTypeScript") {
	dependsOn("compileKotlin")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
