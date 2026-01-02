package com.coupon.storage.rdb.support

import com.github.f4b6a3.tsid.TsidCreator
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * TSID(Time-Sorted Unique Identifier) 생성기
 *
 * TSID는 시간 기반으로 정렬 가능한 고유 식별자를 생성합니다.
 * - 시간 기반 정렬 가능
 * - 분산 환경에서 충돌 없음
 * - Long 타입으로 표현 가능
 * - 데이터베이스 인덱스에 최적화
 *
 * 이 클래스는 thread-safe하며, 애플리케이션 전체에서 재사용 가능합니다.
 */
object TsidGenerator {
    private val lock = ReentrantLock()

    /**
     * 새로운 TSID를 Long 타입으로 생성합니다.
     *
     * @return 생성된 TSID (Long 타입)
     */
    fun generate(): Long =
        lock.withLock {
            TsidCreator.getTsid().toLong()
        }

    /**
     * 새로운 TSID를 String 타입으로 생성합니다.
     *
     * @return 생성된 TSID (String 타입)
     */
    fun generateAsString(): String =
        lock.withLock {
            TsidCreator.getTsid().toString()
        }
}
