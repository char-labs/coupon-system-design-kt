allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation(project(":coupon:coupon-enum"))
    implementation(project(":coupon:coupon-domain"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:${project.properties["lineKotlinJdslVersion"]}")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:${project.properties["lineKotlinJdslVersion"]}")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-boot4-support:${project.properties["lineKotlinJdslVersion"]}")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // TSID
    implementation("com.github.f4b6a3:tsid-creator:5.2.6")

    // Database
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
}
