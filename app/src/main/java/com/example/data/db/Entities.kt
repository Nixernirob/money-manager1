package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val type: String, // "CASH", "BANK", "WALLET", "CREDIT"
    val creditLimit: Double? = null,
    val iconName: String, // material icon identifier
    val tintHex: String // layout accent color hex, e.g., "#C1C5DC"
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val amount: Double,
    val category: String, // "Housing", "Transport", "Food", "Salary", etc.
    val accountId: Int,
    val toAccountId: Int? = null, // for TRANSFERS
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val receiptPath: String? = null, // image uri string if uploaded
    val isRecurring: Boolean = false
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "FORMAL", "SOFT"
    val borrowerOrLender: String, // Name of friend or bank
    val loanAmount: Double,
    val interestRate: Double = 0.0, // yearly APR for formal
    val outstandingAmount: Double,
    val nextDueDate: Long,
    val emiAmount: Double = 0.0, // monthly EMI if formal
    val direction: String, // "LENT" (Assets), "BORROWED" (Liability)
    val isCompleted: Boolean = false
)
