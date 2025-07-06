package com.example.bank.domain.repository

import com.example.bank.domain.entity.AccountReadView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.Optional

@Repository
interface AccountReadViewRepository: JpaRepository<AccountReadView, Long> {
    fun findByAccountNumber(accountNumber: Long): Optional<AccountReadView>

    @Query("SELECT a FROM AccountReadView AS a ORDER BY a.balance DESC")
    fun findAllOrdersByBalanceDesc(): List<AccountReadView>

    @Query("SELECT a FROM AccountReadView AS a WHERE a.balance >= :minBalance")
    fun findByBalanceGreaterThanEqual(
        @Param("minBalance") minBalance : BigDecimal
    ): List<AccountReadView>

    @Query("SELECT a FROM AccountReadView a WHERE a.accountHolderName LIKE %:name%")
    fun findByAccountHolderNameContaining(
        @Param("name") name: String): List<AccountReadView>
}