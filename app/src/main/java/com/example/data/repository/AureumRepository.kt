package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.flow.Flow

class AureumRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val loanDao: LoanDao,
    private val categoryDao: CategoryDao,
    private val softLoanProfileDao: SoftLoanProfileDao,
    private val formalLenderDao: FormalLenderDao
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccountsFlow()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoansFlow()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategoriesFlow()
    val allSoftLoanProfiles: Flow<List<SoftLoanProfile>> = softLoanProfileDao.getAllProfilesFlow()
    val allFormalLenders: Flow<List<FormalLender>> = formalLenderDao.getAllLendersFlow()

    suspend fun getAccount(id: Int): Account? = accountDao.getAccountById(id)

    suspend fun insertSoftLoanProfile(profile: SoftLoanProfile) {
        softLoanProfileDao.insertProfile(profile)
    }

    suspend fun updateSoftLoanProfile(profile: SoftLoanProfile) {
        softLoanProfileDao.updateProfile(profile)
    }

    suspend fun deleteSoftLoanProfile(profile: SoftLoanProfile) {
        softLoanProfileDao.deleteProfile(profile)
    }

    suspend fun insertFormalLender(lender: FormalLender) {
        formalLenderDao.insertLender(lender)
    }

    suspend fun updateFormalLender(lender: FormalLender) {
        formalLenderDao.updateLender(lender)
    }

    suspend fun deleteFormalLender(lender: FormalLender) {
        formalLenderDao.deleteLender(lender)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        // Safe rename if needed:
        // We can rename categories. Any transactions referencing old name should also be renamed.
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        // Re-assign transactions belonging to this category to "Other"
        transactionDao.reassignTransactionsCategory(category.name, "Other")
        categoryDao.deleteCategory(category)
    }

    suspend fun clearAllCategories() {
        categoryDao.clearCategories()
    }

    suspend fun updateCategoryOrders(orderedCategories: List<Category>) {
        orderedCategories.forEachIndexed { index, cat ->
            categoryDao.updateCategory(cat.copy(displayOrder = index))
        }
    }

    suspend fun clearAccounts() {
        accountDao.clearAccounts()
    }

    suspend fun clearLoans() {
        loanDao.clearLoans()
    }

    suspend fun clearSoftLoanProfiles() {
        softLoanProfileDao.clearProfiles()
    }

    suspend fun clearFormalLenders() {
        formalLenderDao.clearLenders()
    }

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account)
    }

    suspend fun clearTransactions() {
        transactionDao.clearTransactions()
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
                            // Deduct the transfer amount plus fee from source
                            val updatedSource = account.copy(balance = account.balance - transaction.amount - transaction.fee)
                            accountDao.insertAccount(updatedSource)
                            // Add the transfer amount to destination
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
                            // Refund source including fee
                            val updatedSource = account.copy(balance = account.balance + transaction.amount + transaction.fee)
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

    suspend fun clearAllTables() {
        accountDao.clearAccounts()
        transactionDao.clearTransactions()
        loanDao.clearLoans()
        categoryDao.clearCategories()
        softLoanProfileDao.clearProfiles()
        formalLenderDao.clearLenders()
    }
}
