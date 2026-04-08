package com.coupon.outbox

import org.springframework.stereotype.Component

@Component
class OutboxEventHandlerRegistry(
    handlers: List<OutboxEventHandler>,
) {
    private val handlersByEventType: Map<String, OutboxEventHandler> =
        handlers
            .groupBy(OutboxEventHandler::eventType)
            .also(::validateDuplicates)
            .mapValues { (_, registeredHandlers) -> registeredHandlers.single() }

    fun find(eventType: String): OutboxEventHandler? = handlersByEventType[eventType]

    fun size(): Int = handlersByEventType.size

    fun eventTypes(): List<String> = handlersByEventType.keys.sorted()

    private fun validateDuplicates(groupedHandlers: Map<String, List<OutboxEventHandler>>) {
        val duplicatedEventTypes = groupedHandlers.filterValues { it.size > 1 }.keys.sorted()

        require(duplicatedEventTypes.isEmpty()) {
            "Duplicate outbox handlers registered for event types: ${duplicatedEventTypes.joinToString()}"
        }
    }
}
