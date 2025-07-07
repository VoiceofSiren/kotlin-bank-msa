package com.example.bank.event.publisher

import com.example.bank.domain.event.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

interface EventPublisher {
    fun publish(event: DomainEvent)
    fun publishAsync(event: DomainEvent)
    fun publishAll(events: List<DomainEvent>)
    fun publishAllAsync(events: List<DomainEvent>)
}

@Component
class EventPublisherImpl(
    // DI
    private val eventPublisher: ApplicationEventPublisher
    // TODO metrics
): EventPublisher {

    private val logger = LoggerFactory.getLogger(EventPublisherImpl::class.java)

    override fun publish(event: DomainEvent) {
        logger.info("publish event: $event")

        try {
            eventPublisher.publishEvent(event)
            // TODO metrics
        } catch (e: Exception) {
            logger.error("publish event error: $event", e)
        }
    }

    @Async("taskExecutor")
    override fun publishAsync(event: DomainEvent) {
        logger.info("publish event: $event")

        try {
            eventPublisher.publishEvent(event)
            // TODO metrics
        } catch (e: Exception) {
            logger.error("publish event error: $event", e)
        }
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { event -> eventPublisher.publishEvent(event) }

    }

    @Async("taskExecutor")
    override fun publishAllAsync(events: List<DomainEvent>) {
        events.forEach { event -> eventPublisher.publishEvent(event) }
    }
}