package com.coupon.user

import com.coupon.shared.keygen.PrefixedKeyGenerator
import org.springframework.stereotype.Component

@Component
class UserKeyGenerator : PrefixedKeyGenerator(prefix = "UK")
