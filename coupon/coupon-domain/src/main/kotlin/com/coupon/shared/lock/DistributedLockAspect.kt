package com.coupon.shared.lock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Aspect
@Component
class DistributedLockAspect(
    private val lock: Lock,
) {
    private val expressionParser = SpelExpressionParser()
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(withDistributedLock)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        withDistributedLock: WithDistributedLock,
    ): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val key = resolveKey(method, joinPoint.target, joinPoint.args, withDistributedLock.key)

        return if (withDistributedLock.requiresNew) {
            lock.withLockRequiresNew(
                key = key,
                timeoutMillis = withDistributedLock.timeoutMillis,
                timeoutException = withDistributedLock.timeoutException,
            ) {
                joinPoint.proceed()
            }
        } else {
            lock.withLock(
                key = key,
                timeoutMillis = withDistributedLock.timeoutMillis,
                timeoutException = withDistributedLock.timeoutException,
            ) {
                joinPoint.proceed()
            }
        }
    }

    private fun resolveKey(
        method: Method,
        target: Any,
        args: Array<Any?>,
        expression: String,
    ): String {
        val evaluationContext = StandardEvaluationContext(target)
        evaluationContext.setVariable("target", target)
        args.forEachIndexed { index, value ->
            evaluationContext.setVariable("p$index", value)
            evaluationContext.setVariable("a$index", value)
        }

        parameterNameDiscoverer.getParameterNames(method)?.forEachIndexed { index, parameterName ->
            evaluationContext.setVariable(parameterName, args.getOrNull(index))
        }

        val evaluated = expressionParser.parseExpression(expression).getValue(evaluationContext)
        return evaluated?.toString() ?: error("Distributed lock key expression returned null: $expression")
    }
}
