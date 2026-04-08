package com.coupon.shared.lock

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
class LockAspectConfig
