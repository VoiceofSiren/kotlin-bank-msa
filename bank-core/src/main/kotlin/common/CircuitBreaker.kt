package com.example.bank.core.common

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CircuitBreakerConfiguration {

    /**
     *  시나리오
     *      1. 최근 5번의 호출 중에서 실패율이 50%를 넘어가 Open 상태로 전환
     *      2. Open 상태에서 30초 동안 대기한 후 Half Open 상태로 전환
     *      3. Half Open 상테에서 3번의 호출을 허용하면서 테스트해보고 어떤 상태로 전환할지를 결정
     */
    @Bean
    fun circuitBreakerRegistry() : CircuitBreakerRegistry {
        val config = CircuitBreakerConfig.custom()
            // 임계값 50: 호출 실패율 50% 달성 시 장애로 판단, open 상태로 전환
            .failureRateThreshold(50f)
            // Open 상태에서 Half Open 상태로 넘어가는 대기 시간
            // Open 상태일 때 모든 호출을 차단하고 30초 동안 대기
            .waitDurationInOpenState(Duration.ofSeconds(30))
            // Half Open 상태에서 허용하는 호출 수를 3으로 설정
            // Half Open 상태에서 Closed 상태로 갈지, Open 상태로 갈지 결정하는 기준값
            .permittedNumberOfCallsInHalfOpenState(3)
            // 최근 5개의 호출을 기준으로 실패율을 계산
            .slidingWindowSize(5)
            // 호출 실패율 계산을 위해 필요한 호출의 최솟값
            .minimumNumberOfCalls(3)
            .build()
        return CircuitBreakerRegistry.of(config)
    }
}

// Singleton pattern
object CircuitBreakerUtils {

    fun <T> CircuitBreaker.execute(
        operation: () -> T,
        fallback: (Exception) -> T
    ) : T {
        return try {
            val supplier = CircuitBreaker.decorateSupplier(this) { operation() }
            supplier.get()
        } catch (e: Exception) {
            fallback(e)
        }
    }
}