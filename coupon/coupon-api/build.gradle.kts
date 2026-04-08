dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    // JWT - jjwt
    implementation("io.jsonwebtoken:jjwt-api:${project.properties["jjwtVersion"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${project.properties["jjwtVersion"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${project.properties["jjwtVersion"]}")

    // Validation
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    implementation(project(":coupon:coupon-enum"))
    implementation(project(":coupon:coupon-domain"))
    implementation(project(":storage:db-core"))
    implementation(project(":storage:redis"))
    implementation(project(":support:logging"))
    implementation(project(":support:monitoring"))

    // OpenApi Spec
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testRuntimeOnly("com.h2database:h2")
}

tasks.named<Jar>("bootJar").configure {
    enabled = true
}

tasks.named<Jar>("jar").configure {
    enabled = false
}
