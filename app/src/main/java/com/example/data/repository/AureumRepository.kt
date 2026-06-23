package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.flow.Flow

class AureumRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val loanDao: LoanDao
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccountsFlow()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoansFlow()

    suspend fun getAccount(id: Int): Account? = accountDao.getAccountById(id)

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account)
    }

    // Handles automatic balance updates on inserting a transaction
    suspend fun insertTransaction(transaction: Transaction) {
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            when (transaction.type) {
                "EXPENSE" -> {
                    val updatedAccount = account.copy(balance = account.balance - transaction.amount)
                    accountDao.insertAccount(updatedAccount)
                }
                "INCOME" -> {
                    val updatedAccount = account.copy(balance = account.balance + transaction.amount)
                    accountDao.insertAccount(updatedAccount)
                }
                "TRANSFER" -> {
                    if (transaction.toAccountId != null) {
                        val destinationAccount = accountDao.getAccountById(transaction.toAccountId)
                        if (destinationAccount != null) {
                            // Deduct from source
                            val updatedSource = account.copy(balance = account.balance - transaction.amount)
                            accountDao.insertAccount(updatedSource)
                            // Add to destination
                            val updatedDestination = destinationAccount.copy(balance = destinationAccount.balance + transaction.amount)
                            accountDao.insertAccount(updatedDestination)
                        }
                    }
                }
            }
        }
        transactionDao.insertTransaction(transaction)
    }

    // Handles automatic balance restoration on deleting a transaction
    suspend fun deleteTransaction(transaction: Transaction) {
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            when (transaction.type) {
                "EXPENSE" -> {
                    val updatedAccount = account.copy(balance = account.balance + transaction.amount)
                    accountDao.insertAccount(updatedAccount)
                }
                "INCOME" -> {
                    val updatedAccount = account.copy(balance = account.balance - transaction.amount)
                    accountDao.insertAccount(updatedAccount)
                }
                "TRANSFER" -> {
                    if (transaction.toAccountId != null) {
                        val destinationAccount = accountDao.getAccountById(transaction.toAccountId)
                        if (destinationAccount != null) {
                            // Refund source
                            val updatedSource = account.copy(balance = account.balance + transaction.amount)
                            accountDao.insertAccount(updatedSource)
                            // Revert destination
                            val updatedDestination = destinationAccount.copy(balance = destinationAccount.balance - transaction.amount)
                            accountDao.insertAccount(updatedDestination)
                        }
                    }
                }
            }
        }
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoanById(id: Int) {
        loanDao.deleteLoanById(id)
    }
}
