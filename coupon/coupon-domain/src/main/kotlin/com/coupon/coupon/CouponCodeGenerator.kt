package com.coupon.coupon

import com.coupon.support.keygen.PrefixedKeyGenerator
import org.springframework.stereotype.Component

@Component
class CouponCodeGenerator : PrefixedKeyGenerator(prefix = "CP", uuidLength = 12)
