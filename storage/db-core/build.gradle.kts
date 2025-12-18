allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation(project(":coupon:coupon-enum"))
    implementation(project(":coupon:coupon-domain"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Database
    runtimeOnly("org.postgresql:postgresql:${project.properties["postgresqlVersion"]}")
    runtimeOnly("com.h2database:h2")
}
