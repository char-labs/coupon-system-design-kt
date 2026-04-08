dependencies {
    implementation("io.sentry:sentry-logback:${project.properties["sentryVersion"]}")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("net.logstash.logback:logstash-logback-encoder:${project.properties["logstashLogbackEncoderVersion"]}")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
}
