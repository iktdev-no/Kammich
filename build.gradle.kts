import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.net.*

plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.24.0"
	id("no.iktdev.ts-gen") version "1.0-rc2"
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
	mavenLocal()
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
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("org.json:json:20231013")

	implementation("org.flywaydb:flyway-core:${flywayVersion}")
	implementation("org.flywaydb:flyway-database-nc-sqlite:${flywayVersion}")
	implementation("org.xerial:sqlite-jdbc:3.45.1.0")
	implementation("com.zaxxer:HikariCP:7.0.2")

	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

	implementation("com.drewnoakes:metadata-extractor:2.19.0")

	// Exposed
	implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")

	// Hash
	implementation("org.lz4:lz4-java:1.8.0")

	implementation("net.coobird:thumbnailator:0.4.20")

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

tsGenerator {
	packageName.set("no.iktdev.kammich.models.shared")
	outputFile.set(file("$projectDir/web/src/types/types.d.ts"))
}

// Definer stier i build-mappen
val immichSpecFile = layout.buildDirectory.file("generated/openapi/immich-openapi-specs.json").get().asFile
val generatedOpenApiDir = layout.buildDirectory.dir("generated/openapi")

val downloadImmichSpec by tasks.registering {
	outputs.file(immichSpecFile)
	doLast {
		// Hopp over nedlasting hvis filen allerede finnes og ikke er tom
		if (immichSpecFile.exists() && immichSpecFile.length() > 0) {
			return@doLast
		}
		immichSpecFile.parentFile.mkdirs()
		val text = URI("https://raw.githubusercontent.com/immich-app/immich/refs/heads/main/open-api/immich-openapi-specs.json").toURL().readText()
		immichSpecFile.writeText(text)
	}
}

tasks.named<GenerateTask>("openApiGenerate") {
	dependsOn(downloadImmichSpec)

	// Fortell Gradle hvilke filer som styrer om tasken er oppdatert
	inputs.file(immichSpecFile)
	outputs.dir(generatedOpenApiDir)

	generatorName.set("kotlin")
	inputSpec.set(immichSpecFile.absolutePath)
	outputDir.set(generatedOpenApiDir.get().asFile.absolutePath)
	apiPackage.set("no.iktdev.kammich.immich.api")
	modelPackage.set("no.iktdev.kammich.immich.models")

	configOptions.set(mapOf(
		"library" to "jvm-okhttp4",
		"serializationLibrary" to "gson",
		"enumPropertyNaming" to "UPPERCASE",
		"dateLibrary" to "java8"
	))

	typeMappings.set(mapOf(
		"integer" to "Long"
	))

	// Fortell generatoren at Long er en innebygd type som ikke skal importeres
	importMappings.set(mapOf(
		"Long" to "kotlin.Long"
	))
}

sourceSets {
	main {
		// Bruk kotlin.srcDir i stedet for java.srcDir
		kotlin.srcDir(generatedOpenApiDir.map { it.dir("src/main/kotlin") })
	}
}


tasks.withType<Test> {
	useJUnitPlatform()
}