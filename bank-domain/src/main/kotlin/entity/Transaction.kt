package com.example.bank.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: TransactionType,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}