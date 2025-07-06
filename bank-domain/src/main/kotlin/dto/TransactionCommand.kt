package com.example.bank.domain.dto

import com.example.bank.domain.entity.TransactionType
import java.math.BigDecimal

data class TransactionCommand(
    val accountNumber: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val description: String
)