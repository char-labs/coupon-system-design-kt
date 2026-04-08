dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-health")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.slack.api:slack-api-client:${project.properties["slackVersion"]}")

    implementation(project(":coupon:coupon-enum"))
    implementation(project(":coupon:coupon-domain"))
    implementation(project(":storage:db-core"))
    implementation(project(":storage:redis"))
    implementation(project(":support:logging"))
    implementation(project(":support:monitoring"))

    testImplementation("org.springframework.security:spring-security-crypto")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:kafka")
}

tasks.named<Jar>("bootJar").configure {
    enabled = true
}

tasks.named<Jar>("jar").configure {
    enabled = false
}
