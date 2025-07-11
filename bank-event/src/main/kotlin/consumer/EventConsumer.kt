package com.example.bank.event.consumer

import com.example.bank.core.common.TxAdvice
import com.example.bank.domain.entity.AccountReadView
import com.example.bank.domain.entity.TransactionReadView
import com.example.bank.domain.event.AccountCreatedEvent
import com.example.bank.domain.event.TransactionCreatedEvent
import com.example.bank.domain.repository.AccountReadViewRepository
import com.example.bank.domain.repository.AccountRepository
import com.example.bank.domain.repository.TransactionReadViewRepository
import com.example.bank.domain.repository.TransactionRepository
import com.example.bank.monitoring.metrics.BankMetrics
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@Component
class EventConsumer(
    private val accountReadViewRepository: AccountReadViewRepository,
    private val transactionReadViewRepository: TransactionReadViewRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val bankMetrics: BankMetrics,
    private val txAdvice: TxAdvice
) {
    private val logger = LoggerFactory.getLogger(EventConsumer::class.java)

    @EventListener
    @Async("taskExecutor")
    @Retryable(value = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun handleAccountCreated(event : AccountCreatedEvent) {
        // API Main -> Publish (TaskExcutor) -> RetryProxy  -> Method -> RetryProxy(1000 밀리초 대기) -> Method
        val startTime = Instant.now()
        val eventType = "AccountCreatedEvent"

        logger.info("event received $eventType")

        try {
            // runNew: @Transactional(propagation = Propagation.REQUIRES_NEW)
            txAdvice.runNew {
                // 계좌 찾기
                val account = accountRepository.findById(event.accountId).orElseThrow {
                    IllegalStateException("Account with id ${event.accountId} not found")
                }

                // CQRS 패턴
                val accountReadView = AccountReadView(
                    id = account.id,
                    accountNumber = account.accountNumber,
                    accountHolderName = account.accountHolderName,
                    balance = account.balance,
                    createdAt = account.createdAt,
                    lastUpdatedAt = LocalDateTime.now(),
                    transactionCount = 0,
                    totalDeposits = BigDecimal.ZERO,
                    totalWithdrawals = BigDecimal.ZERO
                )

                accountReadViewRepository.save(accountReadView)
                logger.info("Account ${account.id} created")
            }

            // 로직 성공 시 처리에 소요된 시간을 계산하여 기록
            val duration = Duration.between(startTime, Instant.now())
            bankMetrics.recordEventProcessingTime(duration, eventType)
            bankMetrics.incrementEventProcessed(eventType)
        } catch (e: Exception) {
            // 로직 실패 시 실패 이벤트 기록
            logger.error("Error occurred while processing $eventType", e)
            bankMetrics.incrementEventFailed(eventType)
            throw e
        }


    }

    @EventListener
    @Async("taskExecutor")
    @Retryable(value = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun handleTransactionCreated(event : TransactionCreatedEvent) {
        val startTime = Instant.now()
        val eventType = "TransactionCreatedEvent"

        logger.info("event received $eventType")

        try {

            // runNew: @Transactional(propagation = Propagation.REQUIRES_NEW)
            txAdvice.runNew {
                // 거래 찾기
                val transaction = transactionRepository.findById(event.transactionId).orElseThrow {
                    IllegalStateException("Transaction with id ${event.transactionId} not found")
                }

                // 계좌 찾기
                val account = accountRepository.findById(event.accountId).orElseThrow {
                    IllegalStateException("Account with id ${event.accountId} not found")
                }

                // CQRS 패턴 - 거래 읽기 전용
                val transactionReadView = TransactionReadView(
                    id = transaction.id,
                    accountId = event.accountId,
                    accountNumber = account.accountNumber,
                    accountHolderName = account.accountHolderName,
                    type = transaction.type,
                    amount = transaction.amount,
                    description = transaction.description,
                    createdAt = transaction.createdAt,
                    balanceAfter = account.balance
                )

                transactionReadViewRepository.save(transactionReadView)
                logger.info("transaction read view updated ${transaction.id}")

                // CQRS 패턴 - 계좌 읽기 전용
                val accountReadView = accountReadViewRepository.findById(account.id).orElseThrow {
                    IllegalStateException("Account with id ${account.id} not found")
                }

                // 계좌 내용 복사
                val updatedAccountReadView = accountReadView.copy(
                    balance = account.balance,
                    lastUpdatedAt = LocalDateTime.now(),
                    transactionCount = accountReadView.transactionCount + 1,
                    totalDeposits = when {
                        transaction.type.name.contains("DEPOSIT") ->
                            accountReadView.totalDeposits + transaction.amount
                        transaction.type.name.contains("TRANSFER") ->
                            accountReadView.totalDeposits + transaction.amount
                        else -> accountReadView.totalDeposits
                    },
                    totalWithdrawals = when {
                        transaction.type.name.contains("WITHDRAWAL") ->
                            accountReadView.totalWithdrawals + transaction.amount
                        transaction.type.name.contains("TRANSFER")->
                            accountReadView.totalDeposits + transaction.amount
                        else -> accountReadView.totalWithdrawals
                    }
                )

                accountReadViewRepository.save(updatedAccountReadView)
                logger.info("Account ${account.id} updated")
            }

            val duration = Duration.between(startTime, Instant.now())
            bankMetrics.recordEventProcessingTime(duration, eventType)
            bankMetrics.incrementEventProcessed(eventType)
        } catch (e : Exception) {
            logger.error("Error occurred while processing $eventType", e)
            bankMetrics.incrementEventFailed(eventType)
            // Retry 사용을 위해 e를 던짐
            throw e
        }

    }
}