package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccountsFlow(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    suspend fun reassignTransactionsCategory(oldCategory: String, newCategory: String)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY nextDueDate ASC")
    fun getAllLoansFlow(): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Int)

    @Query("DELETE FROM loans")
    suspend fun clearLoans()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}

@Dao
interface SoftLoanProfileDao {
    @Query("SELECT * FROM soft_loan_profiles ORDER BY name ASC")
    fun getAllProfilesFlow(): Flow<List<SoftLoanProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SoftLoanProfile): Long

    @Update
    suspend fun updateProfile(profile: SoftLoanProfile)

    @Delete
    suspend fun deleteProfile(profile: SoftLoanProfile)

    @Query("DELETE FROM soft_loan_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    @Query("DELETE FROM soft_loan_profiles")
    suspend fun clearProfiles()
}

@Dao
interface FormalLenderDao {
    @Query("SELECT * FROM formal_lenders ORDER BY name ASC")
    fun getAllLendersFlow(): Flow<List<FormalLender>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLender(lender: FormalLender): Long

    @Update
    suspend fun updateLender(lender: FormalLender)

    @Delete
    suspend fun deleteLender(lender: FormalLender)

    @Query("DELETE FROM formal_lenders WHERE id = :id")
    suspend fun deleteLenderById(id: Int)

    @Query("DELETE FROM formal_lenders")
    suspend fun clearLenders()
}
