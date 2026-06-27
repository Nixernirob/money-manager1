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
    val isRecurring: Boolean = false,
    val fee: Double = 0.0,
    
    // Version 1.2 additions for Soft Loan Integration
    val softLoanProfileId: Int? = null,
    val softLoanType: String? = null, // "BORROW", "GIVE", "REPAY", "RECEIVE"
    val formalLenderId: Int? = null,
    val isLoanActivity: Boolean = false
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
    val isCompleted: Boolean = false,
    val startDate: String = "",
    val dueDateString: String = "",
    val notes: String = "",
    val status: String = "ACTIVE", // "ACTIVE", "PAID", "OVERDUE"
    val paymentsJson: String = "", // Payments log or history
    
    // Version 1.2 additions
    val formalLenderId: Int? = null
)

@Entity(tableName = "soft_loan_profiles")
data class SoftLoanProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val profilePic: String? = null, // "avatar_1", "avatar_2", etc.
    val phoneNumber: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "formal_lenders")
data class FormalLender(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String, // identifier for vector icon, e.g., "wallet", "gift", "utensils"
    val type: String, // "INCOME" or "EXPENSE"
    val isEditable: Boolean = true,
    val isDefault: Boolean = false
)
