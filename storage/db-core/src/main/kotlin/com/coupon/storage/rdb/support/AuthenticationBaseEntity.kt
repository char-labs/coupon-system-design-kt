package com.coupon.storage.rdb.support

import com.coupon.enums.AuthenticationEntityStatus
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(value = [AuditingEntityListener::class])
abstract class AuthenticationBaseEntity {
    @Id
    var id: Long? = null
        protected set

    @Enumerated(value = EnumType.STRING)
    @Column(length = 50)
    var entityStatus: AuthenticationEntityStatus = AuthenticationEntityStatus.ACTIVE
        protected set

    @CreationTimestamp
    @Column(updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    @Column
    var updatedAt: LocalDateTime? = null
        protected set

    @PrePersist
    fun generateId() {
        if (id == null) {
            id = TsidGenerator.generate()
        }
    }

    fun active() {
        entityStatus = AuthenticationEntityStatus.ACTIVE
    }

    fun delete() {
        entityStatus = AuthenticationEntityStatus.DELETE
    }
}
