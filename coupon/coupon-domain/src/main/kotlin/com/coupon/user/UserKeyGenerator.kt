package com.coupon.user

import com.coupon.support.keygen.PrefixedKeyGenerator
import org.springframework.stereotype.Component

@Component
class UserKeyGenerator : PrefixedKeyGenerator(prefix = "UK")
