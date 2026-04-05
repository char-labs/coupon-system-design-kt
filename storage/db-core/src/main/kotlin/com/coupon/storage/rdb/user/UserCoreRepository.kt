package com.coupon.storage.rdb.user

import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.storage.rdb.support.findByIdOrElseThrow
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import com.coupon.user.User
import com.coupon.user.UserCredential
import com.coupon.user.UserProfile
import com.coupon.user.UserRepository
import com.coupon.user.criteria.UserCriteria
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class UserCoreRepository(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(criteria: UserCriteria.Create): User = userJpaRepository.save(UserEntity(criteria)).toUser()

    override fun findById(userId: Long): User = userJpaRepository.findByIdOrElseThrow(userId).toUser()

    override fun findByKey(userKey: String): User =
        userJpaRepository.findByUserKey(userKey)?.toUser()
            ?: throw ErrorException(ErrorType.NOT_FOUND_DATA)

    override fun existsByEmail(email: String): Boolean = userJpaRepository.existsByEmail(email)

    override fun findCredentialByEmail(email: String): UserCredential? = userJpaRepository.findByEmail(email)?.toUserCredential()

    override fun findProfileById(userId: Long): UserProfile {
        val userEntity = userJpaRepository.findByIdOrElseThrow(userId)
        return userEntity.toProfile()
    }

    override fun findAllBy(request: OffsetPageRequest): Page<UserProfile> {
        val pageable = PageRequest.of(request.page, request.size, Sort.by(Sort.Direction.DESC, "id"))
        val page = userJpaRepository.findAllBy(pageable)

        val content = page.content.map { it.toProfile() }
        val totalCount = page.totalElements

        return Page.of(content, totalCount)
    }

    override fun update(
        userId: Long,
        criteria: UserCriteria.Update,
    ): UserProfile {
        val userEntity = userJpaRepository.findByIdOrElseThrow(userId)
        userEntity.update(criteria)
        return userEntity.toProfile()
    }

    override fun delete(userId: Long) {
        val userEntity = userJpaRepository.findByIdOrElseThrow(userId)
        userJpaRepository.delete(userEntity)
    }
}
