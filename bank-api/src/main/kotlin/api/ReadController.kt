package com.example.bank.api

import com.example.bank.common.ApiResponse
import com.example.bank.domain.dto.AccountView
import com.example.bank.domain.dto.TransactionView
import com.example.bank.service.AccountReadService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/api/v1/read/accounts")
@Tag(name = "Read API", description = "Read Operation")
class ReadController(
    private val accountReadService: AccountReadService,
) {

    private val logger = LoggerFactory.getLogger(ReadController::class.java)

    @Operation(
        summary = "accountNumber api",
        description = "accountNumber api",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(mediaType = "application/json",
                    schema = Schema(implementation = AccountView::class)
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "Account Not Found",
                content = [Content(mediaType = "application/json")]
            ),
        ]
    )
    @GetMapping("/{accountNumber}")
    fun getAccount(
        @Parameter(description = "Account number", required = true)
        @PathVariable accountNumber: String
    ): ResponseEntity<ApiResponse<AccountView>> {
        logger.info("read account: $accountNumber")
        return accountReadService.getAccount(accountNumber)
    }

    @Operation(
        summary = "Get Transaction History api",
        description = "Retrieve the transaction history for a specific account",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Transaction History Retrieved Successfully",
                content = [Content(mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]

            ),
        ]
    )
    @GetMapping("/{accountNumber}/transactions")
    fun getTransactionHistory(
        @Parameter(description = "Account number to retrieve transactions for", required = true)
        @PathVariable accountNumber: String,
        @Parameter(description = "Maximum number of transactions to retrieve")
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<ApiResponse<List<TransactionView>>> {
        logger.info("Getting transaction history for account: $accountNumber")
        return accountReadService.transactionHistory(accountNumber, limit)
    }

    @Operation(
        summary = "Get All Accounts api",
        description = "Retrieve all accounts",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "All Accounts Retrieved Successfully",
                content = [Content(mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = AccountView::class))
                )]
            )
        ]
    )
    @GetMapping()
    fun getAllAccounts(): ResponseEntity<ApiResponse<List<AccountView>>> {
        logger.info("Getting all accounts")
        return accountReadService.getAllAccounts()
    }
}