package com.coupon.coupon.support

import com.coupon.enums.error.ErrorType
import com.coupon.support.lock.Lock
import com.coupon.support.lock.LockRepository
import com.coupon.support.tx.Tx

internal object DomainServiceTestRuntime {
    fun initialize(lockRepository: LockRepository = RecordingLockRepository()) {
        Tx(Tx.TxAdvice())
        Lock(Lock.LockAdvice(lockRepository))
    }
}

internal data class LockExecution(
    val key: String,
    val timeoutMillis: Long,
    val timeoutException: ErrorType,
)

internal open class RecordingLockRepository : LockRepository {
    protected val executionLog = mutableListOf<LockExecution>()
    val executions: List<LockExecution> get() = executionLog.toList()

    override fun tryLock(
        key: String,
        timeoutMillis: Long,
    ): Boolean = true

    override fun unlock(key: String) = Unit

    override fun <T> executeWithLock(
        key: String,
        timeoutMillis: Long,
        timeoutException: ErrorType,
        func: () -> T,
    ): T {
        executionLog += LockExecution(key = key, timeoutMillis = timeoutMillis, timeoutException = timeoutException)
        return func()
    }
}
