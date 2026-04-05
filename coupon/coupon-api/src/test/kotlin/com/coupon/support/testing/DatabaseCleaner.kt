package com.coupon.support.testing

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.beans.factory.InitializingBean
import java.sql.DatabaseMetaData
import java.sql.Statement

class DatabaseCleaner(
    private val entityManager: EntityManager,
) : InitializingBean {
    private lateinit var tableNames: List<String>

    override fun afterPropertiesSet() {
        entityManager.unwrap(Session::class.java).doWork { connection ->
            tableNames =
                connection.metaData
                    .findTableNames(connection.catalog, connection.schema)
                    .filterNot(::isIgnoredTable)
        }
    }

    fun clean() {
        if (!::tableNames.isInitialized || tableNames.isEmpty()) {
            return
        }

        entityManager.unwrap(Session::class.java).doWork { connection ->
            connection.createStatement().use { statement ->
                statement.execute("SET REFERENTIAL_INTEGRITY FALSE")
                try {
                    tableNames.forEach { tableName ->
                        statement.truncate(tableName)
                    }
                } finally {
                    statement.execute("SET REFERENTIAL_INTEGRITY TRUE")
                }
            }
        }
    }

    private fun DatabaseMetaData.findTableNames(
        catalog: String?,
        schema: String?,
    ): List<String> =
        getTables(catalog, schema, "%", arrayOf("TABLE")).use { resultSet ->
            buildList {
                while (resultSet.next()) {
                    add(resultSet.getString("TABLE_NAME"))
                }
            }
        }

    private fun Statement.truncate(tableName: String) {
        executeUpdate("TRUNCATE TABLE $tableName")
    }

    private fun isIgnoredTable(tableName: String): Boolean = tableName.equals("flyway_schema_history", ignoreCase = true)
}
