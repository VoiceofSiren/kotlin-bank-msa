package com.example.bank.api

import com.example.bank.common.ApiResponse
import com.example.bank.domain.dto.AccountView
import com.example.bank.request.TransferRequest
import com.example.bank.service.AccountWriteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

data class CreateAccountRequest (
    val name : String,
    val initialBalance : BigDecimal,
)

@RestController
@RequestMapping("/api/v1/write")
@Tag(name = "Write API", description = "Write Operation")
class WriteController (
    private val accountWriteService: AccountWriteService,
) {
    private val logger = LoggerFactory.getLogger(WriteController::class.java)

    @Operation(
        summary = "Create new account",
        description = "Create a new account with specified account holder name and initial balance"
    )
    @PostMapping("/accounts")
    fun createAccount(
        @RequestBody request: CreateAccountRequest
    ): ResponseEntity<ApiResponse<AccountView>> {
        logger.info("Creates account for: ${request.name} with initial balance: ${request.initialBalance}")
        return accountWriteService.createAccount(request.name, request.initialBalance)
    }

    @Operation(
        summary = "Transfer money from one account to another",
        description = "Transfers the specified amount of money from one account to another",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Transfer Successfully"
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "Invalid amount or insufficient funds"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "Account Not Found"
            )
        ]
    )
    @PostMapping("/transactions")
    fun transfer(
        @RequestBody transferRequest: TransferRequest
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("Transfer money from ${transferRequest.fromAccountNumber} to ${transferRequest.toAccountNumber} with amount: ${transferRequest.amount}")
        return accountWriteService.transfer(transferRequest)
    }

}