package com.coupon.user.event

import com.coupon.support.logging.logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 사용자 이벤트 리스너
 * 사용자 삭제와 같은 비동기 작업을 처리한다.
 */
@Component
class UserEventListener {
    private val logger by logger()

    @Async
    @TransactionalEventListener
    fun handleUserDeletedForEmail(event: UserDeletedEvent) {
        logger.info { "사용자 탈퇴 이메일 발송 시작 - userId: ${event.userId}, email: ${event.email}" }
        // 실제로는 이메일 발송 로직이 여기에 들어감
        logger.info { "사용자 탈퇴 이메일 발송 완료 - userId: ${event.userId}" }
    }

    @Async
    @TransactionalEventListener
    fun handleUserDeletedForFiles(event: UserDeletedEvent) {
        logger.info { "사용자 파일 삭제 시작 - userId: ${event.userId}, userKey: ${event.userKey}" }
        // 실제로는 파일 삭제 로직이 여기에 들어감
        logger.info { "사용자 파일 삭제 완료 - userId: ${event.userId}" }
    }

    @Async
    @TransactionalEventListener
    fun handleUserDeletedForCache(event: UserDeletedEvent) {
        logger.info { "사용자 캐시 무효화 시작 - userId: ${event.userId}" }
        // 실제로는 캐시 무효화 로직이 여기에 들어감
        logger.info { "사용자 캐시 무효화 완료 - userId: ${event.userId}" }
    }

    @Async
    @TransactionalEventListener
    fun handleUserDeletedForSlackMessage(event: UserDeletedEvent) {
        logger.info { "사용자 탈퇴 슬랙 알림 시작 - userId: ${event.userId}" }
        // 실제로는 슬랙 알림 전송 로직이 여기에 들어감
        logger.info { "사용자 탈퇴 슬랙 알림 완료 - userId: ${event.userId}" }
    }
}
