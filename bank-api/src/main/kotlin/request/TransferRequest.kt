package com.example.bank.request

import io.swagger.v3.oas.annotations.Parameter
import java.io.Serializable
import java.math.BigDecimal

data class TransferRequest(
    @Parameter(description = "Source account number", required = true)
    val fromAccountNumber: String,
    @Parameter(description = "Destination account number", required = true)
    val toAccountNumber: String,
    @Parameter(description = "Amount to transfer", required = true)
    val amount: BigDecimal
): Serializable