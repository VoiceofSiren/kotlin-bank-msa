package com.example.bank.service

import com.example.bank.common.ApiResponse
import com.example.bank.core.common.TxAdvice
import com.example.bank.domain.dto.AccountView
import com.example.bank.domain.entity.AccountReadView
import com.example.bank.domain.repository.AccountReadViewRepository
import com.example.bank.domain.repository.TransactionReadViewRepository
import common.CircuitBreakerUtils.execute
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springdoc.core.service.OperationService
import org.springframework.http.ResponseEntity

import org.springframework.stereotype.Service

@Service
class AccountReadService (
    private val txAdvice: TxAdvice,
    private val accountReadViewRepository: AccountReadViewRepository,
    private val transactionReadViewRepository: TransactionReadViewRepository,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val operationService: OperationService
) {
    private val logger = LoggerFactory.getLogger(AccountReadService::class.java)
    private val accountReadCircuitBreaker = circuitBreakerRegistry.circuitBreaker("accountRead")

    fun getAccount(accountNumber: String): ResponseEntity<ApiResponse<AccountView>> {
        return accountReadCircuitBreaker.execute(
            operation = {
                txAdvice.readOnly {
                val response = accountReadViewRepository.findByAccountNumber(accountNumber)

                    return@readOnly if (response.isEmpty) {
                        ApiResponse.error("Account Not Found")
                    } else {
                        ApiResponse.success(
                            AccountView.fromReadView(response.get())
                        )
                    }
                }!!
            },
            fallback = { exception ->
                logger.warn("Get Account Failed: ", exception)
                ApiResponse.error<AccountView>(
                    message = "Get Account Failed",
                )
            }
        )
    }
}