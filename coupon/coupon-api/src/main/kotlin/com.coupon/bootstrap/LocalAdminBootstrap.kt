package com.coupon.bootstrap

import com.coupon.enums.AuthorityType
import com.coupon.support.tx.Tx
import com.coupon.user.UserKeyGenerator
import com.coupon.user.UserRepository
import com.coupon.user.criteria.UserCriteria
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@Profile("local")
class LocalAdminBootstrap(
    private val userRepository: UserRepository,
    private val userKeyGenerator: UserKeyGenerator,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${ADMIN_NAME:Load Test Admin}") private val adminName: String,
    @param:Value("\${ADMIN_EMAIL:loadtest-admin@coupon.local}") private val adminEmail: String,
    @param:Value("\${ADMIN_PASSWORD:admin1234!}") private val adminPassword: String,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun run(args: ApplicationArguments) {
        Tx.writeable {
            if (userRepository.existsByEmail(adminEmail)) {
                return@writeable
            }

            userRepository.save(
                UserCriteria.Create(
                    userKey = userKeyGenerator.generate(),
                    name = adminName,
                    email = adminEmail,
                    password = passwordEncoder.encode(adminPassword)!!,
                    role = AuthorityType.ADMIN,
                ),
            )

            log.info("Created local admin bootstrap account for load testing: {}", adminEmail)
        }
    }
}
