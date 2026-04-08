package com.coupon.shared.tx

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class RequiresNewTransactionExecutor {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> run(function: () -> T): T = function()
}
