package com.coupon.coupon

import com.coupon.shared.keygen.PrefixedKeyGenerator
import org.springframework.stereotype.Component

@Component
class CouponCodeGenerator : PrefixedKeyGenerator(prefix = "CP", uuidLength = 12)
