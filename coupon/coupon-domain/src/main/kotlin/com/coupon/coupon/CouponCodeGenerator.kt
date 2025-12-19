package com.coupon.coupon

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class CouponCodeGenerator {
    companion object {
        private val FORMAT_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    fun generate() = "${generateDate()}_CP_${generateUUID()}"

    private fun generateUUID(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 12)

    private fun generateDate(): String = FORMAT_YYYYMMDD.format(LocalDate.now())
}
