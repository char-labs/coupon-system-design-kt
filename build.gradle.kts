import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint") apply false
}

allprojects {
    val projectGroup: String by project
    group = projectGroup
    version = property("projectVersion").toString()

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    dependencies {
        runtimeOnly("org.springframework.boot:spring-boot-starter-validation")
        // Kotlin
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        // Spring
        implementation("org.springframework.boot:spring-boot-starter")
        // Logging
        implementation("io.github.oshai:kotlin-logging-jvm:${project.properties["kotlinLoggingJvmVersion"]}")
        // Serialize (Jackson 3 - Spring Boot 4 default)
        // Spring Boot 4 provides Jackson 3 (tools.jackson.*) by default
        // We only need to add Kotlin module support
        // Note: datatype-jsr310 is now built into jackson-databind in Jackson 3
        implementation(platform("tools.jackson:jackson-bom:3.0.2"))
        implementation("tools.jackson.module:jackson-module-kotlin")

        // Test runtime
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        // Test
        testImplementation(platform("org.testcontainers:testcontainers-bom:${project.properties["testcontainersVersion"]}"))
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("com.ninja-squad:springmockk:${project.properties["springMockkVersion"]}")
        testImplementation("org.mockito:mockito-core:${project.properties["mockitoVersion"]}")
        testImplementation("org.mockito.kotlin:mockito-kotlin:${project.properties["mockitoKotlinVersion"]}")
        testImplementation("org.instancio:instancio-junit:${project.properties["instancioJUnitVersion"]}")
        // Testcontainers
        testImplementation("org.springframework.boot:spring-boot-testcontainers")
        testImplementation("org.testcontainers:testcontainers")
        testImplementation("org.testcontainers:junit-jupiter")
    }

    tasks.withType<Jar> { enabled = true }
    tasks.withType<BootJar> {
        enabled = false
        archiveClassifier.set("boot")
    }

    tasks.test {
        maxParallelForks = 1
        useJUnitPlatform()
        systemProperty("user.timezone", "Asia/Seoul")
        systemProperty("spring.profiles.active", "local")
        jvmArgs("-Xshare:off")
    }

    tasks.withType<JacocoReport> {
        mustRunAfter("test")
        executionData(fileTree(layout.buildDirectory.asFile).include("jacoco/*.exec"))
        reports {
            xml.required = true
            csv.required = false
            html.required = false
        }
        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it)
                    },
                ),
            )
        }
    }

    configure<KtlintExtension> {
        version.set(properties["ktLintVersion"] as String)
    }
}

// 모듈 그룹 프로젝트는 빌드 대상이 아니므로 tasks 비활성화
project(":coupon") { tasks.configureEach { enabled = false } }
project(":storage") { tasks.configureEach { enabled = false } }
project(":support") { tasks.configureEach { enabled = false } }
