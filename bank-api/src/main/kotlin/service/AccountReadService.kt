package com.example.bank.service

import com.example.bank.common.ApiResponse
import com.example.bank.core.common.TxAdvice
import com.example.bank.domain.dto.AccountView
import com.example.bank.domain.dto.TransactionView
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
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("accountRead")

    fun getAccount(accountNumber: String): ResponseEntity<ApiResponse<AccountView>> {
        return circuitBreaker.execute(
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

        fun transactionHistory(accountNumber: String, limit: Int?): ResponseEntity<ApiResponse<List<TransactionView>>> {
            return circuitBreaker.execute(
                operation = {
                  txAdvice.readOnly {
                      val accountReadViewEntity = accountReadViewRepository.findByAccountNumber(accountNumber)
                      if (accountReadViewEntity.isEmpty) {
                          return@readOnly ApiResponse.error("Transaction History Not Found.")
                      }

                      val transactionReadViewEntity = if (limit != null) {
                          transactionReadViewRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber).take(limit)
                      } else {
                          transactionReadViewRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber)
                      }

                      return@readOnly ApiResponse.success(
                          transactionReadViewEntity.map { TransactionView.fromReadView(it)}
                      )
                  }!!
                },
                fallback = { exception ->
                    logger.warn("Get Transaction History Failed: ", exception)
                    ApiResponse.error<List<TransactionView>>(
                        message = "Get Transaction History Failed",
                    )
                }
            )
        }

        fun getAllAcounts(): ResponseEntity<ApiResponse<List<AccountView>>> {
            return circuitBreaker.execute(
                operation = {
                    txAdvice.readOnly {
                        val response = accountReadViewRepository.findAll().map { AccountView.fromReadView(it)}
                        return@readOnly ApiResponse.success(response)
                    }!!
                },
                fallback = { exception ->
                    logger.warn("Get All Accounts Failed: ", exception)
                    ApiResponse.error<List<AccountView>>(
                        message = "Get All Accounts Failed",
                    )
                }
            )
        }
    }
}