package com.coupon.controller.loadtest

import com.coupon.support.tx.Tx
import com.coupon.user.UserKeyGenerator
import com.coupon.user.UserRepository
import com.coupon.user.criteria.UserCriteria
import org.springframework.context.annotation.Profile
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
@Profile("local", "load-test")
class LoadTestSyntheticUserService(
    private val userRepository: UserRepository,
    private val userKeyGenerator: UserKeyGenerator,
    private val passwordEncoder: PasswordEncoder,
) {
    private val encodedDefaultPassword by lazy { passwordEncoder.encode(DEFAULT_PASSWORD)!! }

    /**
     * k6 synthetic user IDs are not real DB primary keys.
     * This resolver maps them to deterministic load-test users so the coupon request flow can keep its FK constraints intact.
     */
    fun resolveUserId(syntheticUserId: Long): Long =
        Tx.writeable {
            val email = syntheticEmailOf(syntheticUserId)

            userRepository.findCredentialByEmail(email)?.userId
                ?: createSyntheticUser(syntheticUserId, email)
        }

    /**
     * Prepares a deterministic synthetic user range before the measured phase starts.
     * This keeps user creation cost out of the actual coupon issuance load window.
     */
    fun prepareUsers(
        startSequence: Long,
        count: Int,
    ): SyntheticUserPreparation {
        require(count >= 0) { "count must be greater than or equal to zero" }

        var preparedCount = 0
        var existingCount = 0

        if (count == 0) {
            return SyntheticUserPreparation(
                startSequence = startSequence,
                requestedCount = count,
                preparedCount = preparedCount,
                existingCount = existingCount,
            )
        }

        for (offset in 0 until count) {
            val syntheticUserId = startSequence + offset
            val email = syntheticEmailOf(syntheticUserId)

            val created =
                Tx.writeable {
                    if (userRepository.findCredentialByEmail(email) != null) {
                        false
                    } else {
                        createSyntheticUser(syntheticUserId, email)
                        true
                    }
                }

            if (created) {
                preparedCount += 1
            } else {
                existingCount += 1
            }
        }

        return SyntheticUserPreparation(
            startSequence = startSequence,
            requestedCount = count,
            preparedCount = preparedCount,
            existingCount = existingCount,
        )
    }

    private fun createSyntheticUser(
        syntheticUserId: Long,
        email: String,
    ): Long =
        try {
            userRepository
                .save(
                    UserCriteria.Create(
                        userKey = userKeyGenerator.generate(),
                        name = "Load Test User $syntheticUserId",
                        email = email,
                        password = encodedDefaultPassword,
                    ),
                ).id
        } catch (_: DataIntegrityViolationException) {
            userRepository.findCredentialByEmail(email)?.userId
                ?: throw IllegalStateException("Load-test synthetic user could not be resolved: $email")
        }

    private fun syntheticEmailOf(syntheticUserId: Long): String = "loadtest-user-$syntheticUserId@coupon.local"

    companion object {
        private const val DEFAULT_PASSWORD = "loadtest-user"
    }

    data class SyntheticUserPreparation(
        val startSequence: Long,
        val requestedCount: Int,
        val preparedCount: Int,
        val existingCount: Int,
    )
}
