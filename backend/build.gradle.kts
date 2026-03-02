import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.allopen") version "2.3.10"
    id("io.quarkus")
    id("org.jooq.jooq-codegen-gradle") version "3.20.11"
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val kotestVersion = "5.9.1"
val jooqVersion = "3.20.11"

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation(kotlin("stdlib"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("org.jooq:jooq:$jooqVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-hibernate-validator")

    jooqCodegen("org.jooq:jooq-codegen:$jooqVersion")
    jooqCodegen("org.jooq:jooq-meta-extensions:$jooqVersion")
    jooqCodegen("com.h2database:h2:2.3.232")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("org.wiremock:wiremock:3.12.1")
}

group = "com.meet"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        javaParameters = true
    }
}

ktlint {
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.enterprise.context.RequestScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

jooq {
    configuration {
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/db/migration/*.sql"
                    }
                    property {
                        key = "sort"
                        value = "flyway"
                    }
                    property {
                        key = "unqualifiedSchema"
                        value = "none"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
            generate {
                isPojosAsKotlinDataClasses = true
                isKotlinNotNullPojoAttributes = true
                isKotlinNotNullRecordAttributes = true
                isKotlinDefaultedNullablePojoAttributes = true
                isKotlinDefaultedNullableRecordAttributes = true
                isDaos = false
                isRecords = true
                isFluentSetters = true
                isPojos = true
            }
            target {
                packageName = "com.meet.generated.jooq"
                directory = "build/generated-sources/jooq"
            }
        }
    }
}

tasks.named("jooqCodegen") {
    inputs
        .files(fileTree("src/main/resources/db/migration"))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

sourceSets {
    main {
        kotlin {
            srcDir("build/generated-sources/jooq")
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("jooqCodegen")
}

tasks.matching { it.name.startsWith("runKtlintCheckOver") }.configureEach {
    mustRunAfter("jooqCodegen")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
