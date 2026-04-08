dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.kafka:spring-kafka")

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
