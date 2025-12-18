package com.coupon.storage.rdb.user

import com.coupon.enums.AuthorityType
import com.coupon.storage.rdb.support.BaseEntity
import com.coupon.user.User
import com.coupon.user.UserCredential
import com.coupon.user.UserProfile
import com.coupon.user.criteria.UserCriteria
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "t_user",
    indexes = [
        Index(name = "idx_user_user_key", columnList = "user_key"),
    ],
)
class UserEntity(
    @Column(name = "user_key")
    val userKey: String,
    var name: String,
    var email: String,
    var password: String,
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(10)")
    var role: AuthorityType = AuthorityType.USER,
) : BaseEntity() {
    constructor(
        criteria: UserCriteria.Create,
    ) : this(
        userKey = criteria.userKey,
        name = criteria.name,
        email = criteria.email,
        password = criteria.password,
        role = criteria.role,
    )

    fun toUser() =
        User(
            id = id!!,
            key = userKey,
            role = role,
        )

    fun toProfile() =
        UserProfile(
            id = id!!,
            key = userKey,
            name = name,
            email = email,
            role = role,
            createdAt = createdAt,
        )

    fun update(criteria: UserCriteria.Update) {
        criteria.name?.let { this.name = it }
        criteria.email?.let { this.email = it }
        criteria.password?.let { this.password = it }
    }

    fun toUserCredential() =
        UserCredential(
            userId = id!!,
            userKey = userKey,
            email = email,
            password = password,
        )
}
