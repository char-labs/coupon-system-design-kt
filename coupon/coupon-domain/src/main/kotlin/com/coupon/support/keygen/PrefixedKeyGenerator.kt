package com.coupon.support.keygen

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

abstract class PrefixedKeyGenerator(
    private val prefix: String,
    private val uuidLength: Int = 32,
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun generate(): String = "${generateDate()}_${prefix}_${generateUUID()}"

    private fun generateUUID(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(uuidLength)

    private fun generateDate(): String = formatter.format(LocalDate.now())
}
