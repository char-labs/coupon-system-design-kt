package com.coupon.user

import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import com.coupon.support.tx.Tx
import com.coupon.user.command.UserCommand
import com.coupon.user.criteria.UserCriteria
import com.coupon.user.event.UserDeletedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userKeyGenerator: UserKeyGenerator,
    private val eventPublisher: ApplicationEventPublisher,
) {
    companion object {
        /** 이메일 정규화 **/
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+.[A-Za-z]{2,}\$")
    }

    fun createUser(command: UserCommand.Create): User = userRepository.save(UserCriteria.Create.of(userKeyGenerator.generate(), command))

    /**
     * 사용자 ID로 조회
     * 캐싱 적용 가능: @Cacheable(value = ["user"], key = "#userId")
     */
    fun getUser(userId: Long): User = userRepository.findById(userId)

    /**
     * 사용자 Key로 조회
     * 캐싱 적용 가능: @Cacheable(value = ["user"], key = "#userKey")
     */
    fun getUser(userKey: String): User = userRepository.findByKey(userKey)

    fun verifyEmail(email: String) {
        if (!email.matches(EMAIL_REGEX)) {
            throw ErrorException(ErrorType.INVALID_LOGIN_ID_FORMAT)
        }

        if (userRepository.existsByEmail(email)) {
            throw ErrorException(ErrorType.DUPLICATED_EMAIL)
        }
    }

    fun getCredentialByEmail(email: String): UserCredential =
        userRepository.findCredentialByEmail(email)
            ?: throw ErrorException(ErrorType.NOT_FOUND_DATA)

    fun getProfile(userId: Long): UserProfile = userRepository.findProfileById(userId)

    fun getUsers(request: OffsetPageRequest): Page<UserProfile> = userRepository.findAllBy(request)

    fun modifyUser(
        userId: Long,
        command: UserCommand.Update,
    ): UserProfile =
        Tx.writeable {
            userRepository.update(userId, UserCriteria.Update.of(command))
        }

    fun deleteUser(userId: Long) =
        Tx.writeable {
            val user = userRepository.findProfileById(userId)
            userRepository.delete(user.id)

            // 삭제 이벤트 발행 (비동기 처리)
            eventPublisher.publishEvent(
                UserDeletedEvent(
                    userId = user.id,
                    userKey = user.key,
                    email = user.email,
                ),
            )
        }
}
