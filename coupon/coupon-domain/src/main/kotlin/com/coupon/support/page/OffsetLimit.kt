package com.coupon.support.page

import com.coupon.enums.Sort
import com.coupon.enums.SortType

data class OffsetLimit(
    val offset: Long,
    val limit: Long,
    val type: SortType? = SortType.NEW,
    val sort: Sort? = Sort.DESC,
) {
    init {
        require(offset >= 0) { "offset은 0보다 커야 한다. " }
        require(limit > 0) { "limit은 0보다 커야 한다. " }
    }

    companion object {
        fun of(
            offset: Long,
            limit: Long,
            type: SortType? = SortType.NEW,
            sort: Sort? = Sort.DESC,
        ): OffsetLimit = OffsetLimit(offset, limit, type, sort)
    }
}
