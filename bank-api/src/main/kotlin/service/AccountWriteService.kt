package com.example.bank.service

import com.example.bank.common.ApiResponse
import com.example.bank.core.common.CircuitBreakerUtils.execute
import com.example.bank.core.common.TxAdvice
import com.example.bank.core.lock.DistributedLockService
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
import com.example.bank.request.TransferRequest
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal

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
        transferRequest: TransferRequest
    ): ResponseEntity<ApiResponse<String>> {
        val fromAccountNumber = transferRequest.fromAccountNumber
        val toAccountNumber = transferRequest.toAccountNumber
        val amount = transferRequest.amount
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
        val transactionResult = txAdvice.run {
            val fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)

            if (fromAccount == null) {
                return@run null to "FromAccount Not Found"
            }

            if (fromAccount.balance < amount) {
                return@run null to "Insufficient Balance"
            }

            val toAccount = accountRepository.findByAccountNumber(toAccountNumber)

            if (toAccount == null) {
                return@run null to "ToAccount Not Found"
            }

            fromAccount.balance = fromAccount.balance.subtract(amount)
            toAccount.balance = toAccount.balance.add(amount)

            val fromAccountSaved = accountRepository.save(fromAccount)
            val toAccountSaved = accountRepository.save(toAccount)

            // [1] 거래 이벤트 처리: From
            val fromTransaction = Transaction(
                account = fromAccount,
                amount = amount,
                type = TransactionType.TRANSFER,
                description = "Transfer From $fromAccountNumber"
            )
            val fromTransactionSaved = transactionRepository.save(fromTransaction)
            bankMetrics.incrementTransaction("TRANSFER")


            // [2] 거래 이벤트 처리: To
            val toTransaction = Transaction(
                account = toAccount,
                amount = amount,
                type = TransactionType.TRANSFER,
                description = "Transfer To $toAccountNumber"
            )
            val toTransactionSaved = transactionRepository.save(toTransaction)
            bankMetrics.incrementTransaction("TRANSFER")


            return@run Pair(
                listOf(
                    Pair(fromTransactionSaved, fromAccountSaved),
                    Pair(toTransactionSaved, toAccountSaved)
                ),
                null
            )
        }!!

        if (transactionResult.first == null) {
            return ApiResponse.error<String>(transactionResult.second!!)
        }

        transactionResult.first!!.forEach { (transactionSaved, accountSaved) ->
            // [3] 이벤트 발행: From, To
            eventPublisher.publishAsync(
                TransactionCreatedEvent(
                    transactionId = transactionSaved.id,
                    accountId = accountSaved.id,
                    type = transactionSaved.type,
                    amount = amount,
                    description = "Transaction Created",
                    balanceAfter = accountSaved.balance
                )
            )
        }

        return ApiResponse.success<String>(
            data = "Transfer Completed",
            message = "Transfer Completed"
        )
    }
}