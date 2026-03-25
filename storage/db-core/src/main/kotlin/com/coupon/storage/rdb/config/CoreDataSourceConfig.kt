package com.coupon.storage.rdb.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class CoreDataSourceConfig {
    @Bean
    @ConfigurationProperties(prefix = "datasource.db.core")
    fun coreHikariConfig(): HikariConfig = HikariConfig()

    @Bean
    fun coreDataSource(
        @Qualifier("coreHikariConfig") config: HikariConfig,
        flywayProperties: CoreFlywayProperties,
    ): HikariDataSource =
        HikariDataSource(config).also { dataSource ->
            if (flywayProperties.enabled) {
                Flyway
                    .configure()
                    .dataSource(dataSource)
                    .baselineOnMigrate(flywayProperties.baselineOnMigrate)
                    .locations(*flywayProperties.locations.toTypedArray())
                    .load()
                    .migrate()
            }
        }
}

@ConfigurationProperties(prefix = "spring.flyway")
data class CoreFlywayProperties(
    var enabled: Boolean = true,
    var baselineOnMigrate: Boolean = false,
    var locations: List<String> = listOf("classpath:db/migration"),
)
