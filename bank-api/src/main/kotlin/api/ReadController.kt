package com.example.bank.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/read")
@Tag(name = "Read API", description = "read operation")
class ReadController {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Operation(
        summary = "accountNumber api",
        description = "accountNumber api",
        responses = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(mediaType = "application/json")]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "BAD REQUEST",
                content = [Content(mediaType = "application/json")]
            ),
        ]
    )
    @GetMapping("/{accountNumber}")
    fun getAccount(
        @Parameter(description = "Account number", required = true)
        @PathVariable accountNumber: String
    ): String {
        logger.info("read account: $accountNumber")
        return "read account: $accountNumber"
    }


}