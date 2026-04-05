package com.coupon.storage.rdb.support

import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull

fun <T : BaseEntity> JpaRepository<T, Long>.findByIdOrElseThrow(id: Long): T {
    val value = findByIdOrNull(id) ?: throw ErrorException(ErrorType.NOT_FOUND_DATA)
    return value
}

fun <T : BaseEntity> JpaRepository<T, Long>.findByIdAndDeletedAtIsNullOrElseThrow(id: Long): T {
    val value = findByIdOrNull(id).takeIf { it?.deletedAt == null } ?: throw ErrorException(ErrorType.NOT_FOUND_DATA)
    return value
}
