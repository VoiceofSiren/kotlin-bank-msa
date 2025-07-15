package com.example.bank.service

import com.example.bank.common.ApiResponse
import com.example.bank.core.common.TxAdvice
import com.example.bank.domain.dto.AccountView
import com.example.bank.domain.entity.Account
import com.example.bank.domain.entity.Transaction
import com.example.bank.domain.entity.TransactionType
import com.example.bank.domain.event.AccountCreatedEvent
import com.example.bank.domain.event.TransactionCreatedEvent
import com.example.bank.domain.repository.AccountRepository
import com.example.bank.domain.repository.TransactionRepository
import com.example.bank.event.publisher.EventPublisher
import com.example.bank.monitoring.metrics.BankMetrics
import com.lecture.bank.core.lock.DistributedLockService
import common.CircuitBreakerUtils.execute
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class AccountWriteService(
    private val txAdvice: TxAdvice,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val lockService: DistributedLockService,
    private val eventPublisher: EventPublisher,
    private val bankMetrics: BankMetrics
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(AccountWriteService::class.java)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("accountWrite")

    private fun randomAccountNumber(): String {
        return System.currentTimeMillis().toString()
    }

    /**
     *  Account를 생성하는 비즈니스 로직
     */
    fun createAccount(
        name: String,
        balance: BigDecimal
    ): ResponseEntity<ApiResponse<AccountView>> {
        return circuitBreaker.execute(
            operation = {
                val account = txAdvice.run {
                    val accountNumber = randomAccountNumber()
                    val account = Account(
                        accountNumber = accountNumber,
                        balance = balance,
                        accountHolderName = name
                    )
                    accountRepository.save(account)
                }!!
                bankMetrics.incrementAccountCreated()
                bankMetrics.updateAccountCount(accountRepository.count())
                eventPublisher.publishAsync(
                    AccountCreatedEvent(
                        accountId = account.id,
                        accountNumber = account.accountNumber,
                        accountHolderName = account.accountHolderName,
                        initialBalance = account.balance
                    )
                )
                return@execute ApiResponse.success(
                    data = AccountView(
                        id = account.id,
                        accountNumber = account.accountNumber,
                        balance = account.balance,
                        accountHolderName = account.accountHolderName,
                        createdAt = account.createdAt
                    ),
                    message = "Account Created"
                )
            },
            fallback = { exception ->
                logger.warn("Create Account Failed: ", exception)
                ApiResponse.error<AccountView>(
                    message = "Create Account Failed",
                )
            }
        )
    }

    fun transfer(
        fromAccountNumber: String,
        toAccountNumber: String,
        amount: BigDecimal
    ): ResponseEntity<ApiResponse<String>> {
        return circuitBreaker.execute(
            operation = {
                // 동시성 제어
                lockService.executeWithTransactionLock(fromAccountNumber, toAccountNumber) {
                    transferInternal(fromAccountNumber, toAccountNumber, amount)
                }
            },
            fallback = { exception ->
                logger.warn("Transfer Failed: ", exception)
                ApiResponse.error<String>(
                    message = "Transfer Failed",
                )
            }
        )!!
    }

    private fun transferInternal(
        fromAccountNumber: String,
        toAccountNumber: String,
        amount: BigDecimal
    ): ResponseEntity<ApiResponse<String>>? {
        return txAdvice.run {
            val fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)

            if (fromAccount == null) {
                return@run ApiResponse.error("FromAccount Not Found")
            }

            if (fromAccount.balance < amount) {
                return@run ApiResponse.error("Insufficient Balance")
            }

            val toAccount = accountRepository.findByAccountNumber(toAccountNumber)

            if (toAccount == null) {
                return@run ApiResponse.error("ToAccount Not Found")
            }

            fromAccount.balance = fromAccount.balance.subtract(amount)
            toAccount.balance = toAccount.balance.add(amount)

            val fromAccountSaved = accountRepository.save(fromAccount)
            val toAccountSaved = accountRepository.save(toAccount)

            // 거래 이벤트 처리 (1): From
            val fromTransaction = Transaction(
                account = fromAccount,
                amount = amount,
                type = TransactionType.TRANSFER,
                description = "Transfer From $fromAccountNumber"
            )
            val fromTransactionId = transactionRepository.save(fromTransaction).id!!
            bankMetrics.incrementTransaction("TRANSFER")
            eventPublisher.publishAsync(
                TransactionCreatedEvent(
                    transactionId = fromTransactionId,
                    accountId = fromAccountSaved.id,
                    type = fromTransaction.type,
                    amount = amount,
                    description = "Transaction Created",
                    balanceAfter = fromAccountSaved.balance
                )
            )

            // 거래 이벤트 처리 (1): To
            val toTransaction = Transaction(
                account = toAccount,
                amount = amount,
                type = TransactionType.TRANSFER,
                description = "Transfer To $toAccountNumber"
            )
            val toTransactionId = transactionRepository.save(toTransaction).id!!
            bankMetrics.incrementTransaction("TRANSFER")
            eventPublisher.publishAsync(
                TransactionCreatedEvent(
                    transactionId = toTransactionId,
                    accountId = toAccountSaved.id,
                    type = toTransaction.type,
                    amount = amount,
                    description = "Transaction Created",
                    balanceAfter = toAccountSaved.balance
                )
            )

            return@run ApiResponse.success(
                data = "Transfer Completed",
                message = "Transfer Completed"
            )
        }
    }
}