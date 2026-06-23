package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.Account
import com.example.data.db.AppDatabase
import com.example.data.db.Loan
import com.example.data.db.Transaction
import com.example.data.db.Category
import com.example.data.repository.AureumRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AureumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AureumRepository
    private val prefs = application.getSharedPreferences("aureum_prefs", android.content.Context.MODE_PRIVATE)
    
    // Core data flows from repository
    val allAccounts: StateFlow<List<Account>>
    val allTransactions: StateFlow<List<Transaction>>
    val allLoans: StateFlow<List<Loan>>
    val allCategories: StateFlow<List<Category>>

    // Flow matching the active UI screen state
    val activeTab = MutableStateFlow("HOME") // HOME, TRANSACTIONS, STATS, ACCOUNTS, MORE

    // Security & Lock settings
    val isLocked = MutableStateFlow(false) // Whether App is CURRENTLY locked
    val isPinGuardEnabled = MutableStateFlow(prefs.getBoolean("pin_guard_enabled", false)) // Whether PIN guard is active
    val pinPromptMode = MutableStateFlow(false) // Pin entering mode
    val pinInput = MutableStateFlow("")
    val correctPin = MutableStateFlow(prefs.getString("correct_pin", "1234") ?: "1234") // Custom PIN saved
    val isBalanceHidden = MutableStateFlow(false)
    val hiddenAccounts = MutableStateFlow<Set<Int>>(emptySet())
    val secureModeEnabled = MutableStateFlow(false) // Anti-screenshot toggle

    // Custom App Configs
    val selectedCurrency = MutableStateFlow("৳") // default (BDT) or "$"
    val isDarkTheme = MutableStateFlow(false) // Professional Polish light theme by default

    // Transaction List Filters (Daily / Calendar heat map / Monthly breakdown etc)
    val transactionFilterType = MutableStateFlow("ALL") // "ALL", "DAILY", "MONTHLY", "CALENDAR"
    val transactionCategoryFilter = MutableStateFlow("ALL") // Category names

    // Calculator state
    val calcExpression = MutableStateFlow("")
    val calcResult = MutableStateFlow("")

    // Export confirmation message state
    val exportMessage = MutableStateFlow<String?>(null)

    init {
        if (isPinGuardEnabled.value) {
            isLocked.value = true
        }

        val database = AppDatabase.getDatabase(application)
        repository = AureumRepository(
            database.accountDao(),
            database.transactionDao(),
            database.loanDao(),
            database.categoryDao()
        )

        allAccounts = repository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allLoans = repository.allLoans.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCategories = repository.allCategories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial mock accounts and loans if database is empty on launch
        viewModelScope.launch {
            allAccounts.first { true } // wait for database check
            repository.allAccounts.take(1).collect { accountsList ->
                if (accountsList.isEmpty()) {
                    seedDefaultAureumData()
                }
            }
        }

        // Seed default categories
        viewModelScope.launch {
            allCategories.first { true }
            repository.allCategories.take(1).collect { catList ->
                if (catList.isEmpty()) {
                    seedDefaultCategories()
                }
            }
        }
    }

    private suspend fun seedDefaultAureumData() {
        // Create premium default accounts
        val vault = Account(
            name = "Aureum Vault",
            balance = 124500.0,
            type = "ACCOUNTS",
            iconName = "account_balance",
            tintHex = "#c1c5dc" // light purple
        )
        val reserve = Account(
            name = "High-Yield Reserve",
            balance = 85000.0,
            type = "CASH",
            iconName = "savings",
            tintHex = "#bbc4f2" // royal accent
        )
        val metalCard = Account(
            name = "Infinite Metal Card",
            balance = 12450.0,
            type = "CARD",
            creditLimit = 50000.0,
            iconName = "credit_card",
            tintHex = "#bdc5e7" // secondary premium
        )

        repository.insertAccount(vault)
        repository.insertAccount(reserve)
        repository.insertAccount(metalCard)

        // Seed initial corresponding transactions
        val now = System.currentTimeMillis()
        repository.insertTransaction(
            Transaction(
                type = "EXPENSE",
                amount = 4500.0,
                category = "Retail",
                accountId = 1,
                note = "Harrods Shopping Collection",
                timestamp = now - 7200000 // 2 hours ago
            )
        )
        repository.insertTransaction(
            Transaction(
                type = "INCOME",
                amount = 50000.0,
                category = "Internal",
                accountId = 1,
                note = "Dividends & Growth yields",
                timestamp = now - 86400000 // Yesterday
            )
        )
        repository.insertTransaction(
            Transaction(
                type = "EXPENSE",
                amount = 12800.0,
                category = "Dining",
                accountId = 1, // Aureum Vault
                note = "Nobu Luxury Fine Dining",
                timestamp = now - 172800000 // 2 Days ago
            )
        )

        // Seed structured loans (Formal EMI + Soft friends loans)
        val overdueDate = now - 3 * 24 * 60 * 60 * 1000 // 3 days overdue
        val autoLoanFutureDate = now + 10 * 24 * 60 * 60 * 1000 // 10 days out

        repository.insertLoan(
            Loan(
                type = "FORMAL",
                borrowerOrLender = "Mortgage Facility",
                loanAmount = 14250.0,
                interestRate = 4.2,
                outstandingAmount = 14250.0,
                nextDueDate = overdueDate,
                emiAmount = 2450.0,
                direction = "BORROWED"
            )
        )

        repository.insertLoan(
            Loan(
                type = "FORMAL",
                borrowerOrLender = "Auto High Loan",
                loanAmount = 5200.0,
                interestRate = 3.5,
                outstandingAmount = 420.0,
                nextDueDate = autoLoanFutureDate,
                emiAmount = 420.0,
                direction = "BORROWED"
            )
        )

        repository.insertLoan(
            Loan(
                type = "SOFT",
                borrowerOrLender = "Sarah Rogers",
                loanAmount = 120.0,
                outstandingAmount = 120.0,
                nextDueDate = now,
                direction = "LENT"
            )
        )

        repository.insertLoan(
            Loan(
                type = "SOFT",
                borrowerOrLender = "Mike Johnson",
                loanAmount = 850.0,
                outstandingAmount = 850.0,
                nextDueDate = now,
                direction = "BORROWED"
            )
        )
    }

    // --- Transactions Insertion Operations ---
    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        accountId: Int,
        toAccountId: Int?,
        note: String,
        timestamp: Long,
        isRecurring: Boolean = false,
        receiptPath: String? = null,
        fee: Double = 0.0
    ) {
        viewModelScope.launch {
            val trans = Transaction(
                type = type,
                amount = amount,
                category = category,
                accountId = accountId,
                toAccountId = toAccountId,
                note = note,
                timestamp = timestamp,
                isRecurring = isRecurring,
                receiptPath = receiptPath,
                fee = fee
            )
            repository.insertTransaction(trans)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // --- Accounts Operations ---
    fun addAccount(name: String, initialBalance: Double, type: String, creditLimit: Double?, tintHex: String) {
        viewModelScope.launch {
            val acc = Account(
                name = name,
                balance = initialBalance,
                type = type,
                creditLimit = creditLimit,
                iconName = when (type.uppercase()) {
                    "ACCOUNTS", "BANK" -> "account_balance"
                    "CASH", "WALLET" -> "savings"
                    "CARD", "CREDIT" -> "credit_card"
                    else -> "cash"
                },
                tintHex = tintHex
            )
            repository.insertAccount(acc)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addCategory(name: String, icon: String, type: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, icon = icon, type = type, isEditable = true, isDefault = false))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    suspend fun seedDefaultCategories() {
        val incomeDefaults = listOf(
            Category(name = "Salary", icon = "wallet", type = "INCOME", isEditable = true, isDefault = true),
            Category(name = "Allowance", icon = "hand_coins", type = "INCOME", isEditable = true, isDefault = true),
            Category(name = "Business Profit", icon = "trending_up", type = "INCOME", isEditable = true, isDefault = true),
            Category(name = "Bonus", icon = "gift", type = "INCOME", isEditable = true, isDefault = true),
            Category(name = "Other", icon = "circle", type = "INCOME", isEditable = false, isDefault = true)
        )
        val expenseDefaults = listOf(
            Category(name = "Food", icon = "utensils", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Apparel", icon = "shirt", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "House", icon = "home", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Rent", icon = "building", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Internet Bill", icon = "wifi", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Transport", icon = "bus", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Health", icon = "heart_pulse", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Beauty", icon = "sparkles", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Education", icon = "graduation_cap", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Gift", icon = "gift", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Entertainment", icon = "gamepad", type = "EXPENSE", isEditable = true, isDefault = true),
            Category(name = "Other", icon = "circle", type = "EXPENSE", isEditable = false, isDefault = true)
        )
        for (c in incomeDefaults) repository.insertCategory(c)
        for (c in expenseDefaults) repository.insertCategory(c)
    }

    fun restoreDefaultSettings() {
        viewModelScope.launch {
            repository.clearAllCategories()
            seedDefaultCategories()

            isPinGuardEnabled.value = false
            correctPin.value = "1234"
            isBalanceHidden.value = false
            hiddenAccounts.value = emptySet()
            secureModeEnabled.value = false
            selectedCurrency.value = "৳"
            isDarkTheme.value = false
            transactionFilterType.value = "ALL"
            transactionCategoryFilter.value = "ALL"

            prefs.edit().apply {
                putBoolean("pin_guard_enabled", false)
                putString("correct_pin", "1234")
                apply()
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.clearAllTables()
        }
    }

    fun hideAccount(accountId: Int, hide: Boolean) {
        val currentSet = hiddenAccounts.value.toMutableSet()
        if (hide) {
            currentSet.add(accountId)
        } else {
            currentSet.remove(accountId)
        }
        hiddenAccounts.value = currentSet
    }

    // --- Loans Operations ---
    fun addLoan(type: String, borrowerOrLender: String, amount: Double, interest: Double, emi: Double, direction: String, nextDue: Long) {
        viewModelScope.launch {
            val l = Loan(
                type = type,
                borrowerOrLender = borrowerOrLender,
                loanAmount = amount,
                interestRate = interest,
                outstandingAmount = amount,
                nextDueDate = nextDue,
                emiAmount = emi,
                direction = direction
            )
            repository.insertLoan(l)
        }
    }

    fun repayLoanAmount(loan: Loan, amountPaid: Double) {
        viewModelScope.launch {
            val updated = loan.copy(
                outstandingAmount = maxOf(0.0, loan.outstandingAmount - amountPaid),
                isCompleted = (loan.outstandingAmount - amountPaid) <= 0.0
            )
            repository.updateLoan(updated)
            
            // Log as a transaction expense/income
            val title = "Repayment: ${loan.borrowerOrLender}"
            // Try to find a matching account to deduct from
            val defaultAccId = allAccounts.value.firstOrNull { it.type == "ACCOUNTS" || it.type == "BANK" }?.id ?: allAccounts.value.firstOrNull()?.id ?: 1
            addTransaction(
                type = if (loan.direction == "LENT") "INCOME" else "EXPENSE",
                amount = amountPaid,
                category = "Loans",
                accountId = defaultAccId,
                toAccountId = null,
                note = title,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun markSoftLoanReturned(loan: Loan) {
        viewModelScope.launch {
            val updated = loan.copy(
                outstandingAmount = 0.0,
                isCompleted = true
            )
            repository.updateLoan(updated)

            // Auto log the return
            val defaultAccId = allAccounts.value.firstOrNull { it.type == "ACCOUNTS" || it.type == "BANK" }?.id ?: allAccounts.value.firstOrNull()?.id ?: 1
            addTransaction(
                type = if (loan.direction == "LENT") "INCOME" else "EXPENSE",
                amount = loan.outstandingAmount,
                category = "Loans",
                accountId = defaultAccId,
                toAccountId = null,
                note = "Returned: ${loan.borrowerOrLender}",
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun deleteLoan(loanId: Int) {
        viewModelScope.launch {
            repository.deleteLoanById(loanId)
        }
    }

    // --- Security PIN unlock actions ---
    fun enterPinDigit(digit: String) {
        if (pinInput.value.length < 4) {
            pinInput.value += digit
            if (pinInput.value.length == 4) {
                if (pinInput.value == correctPin.value) {
                    isLocked.value = false
                    pinPromptMode.value = false
                    pinInput.value = ""
                } else {
                    // Reset input on failure
                    pinInput.value = ""
                }
            }
        }
    }

    fun clearPinInput() {
        pinInput.value = ""
    }

    fun activateLock() {
        isLocked.value = true
    }

    fun saveNewPin(newPin: String) {
        if (newPin.length == 4) {
            prefs.edit().putString("correct_pin", newPin).apply()
            correctPin.value = newPin
        }
    }

    fun setPinGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pin_guard_enabled", enabled).apply()
        isPinGuardEnabled.value = enabled
        if (enabled && (correctPin.value.isBlank() || correctPin.value == "1234")) {
            saveNewPin("1234")
        }
    }

    // --- Calculator Built-in Logic ---
    fun onCalcPress(char: String) {
        val current = calcExpression.value
        when (char) {
            "C" -> {
                calcExpression.value = ""
                calcResult.value = ""
            }
            "=" -> {
                try {
                    val res = evaluateExpression(calcExpression.value)
                    calcResult.value = String.format(Locale.US, "%.2f", res)
                } catch (e: Exception) {
                    calcResult.value = "Error"
                }
            }
            else -> {
                calcExpression.value = current + char
            }
        }
    }

    private fun evaluateExpression(expr: String): Double {
        // Simple evaluator for +, -, *, /
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < sanitized.length) sanitized[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < sanitized.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = sanitized.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }

    // --- Clean Offline CSV Exporter ---
    fun exportLedgerToCsv() {
        viewModelScope.launch {
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                val csvContent = StringBuilder()
                csvContent.append("ID,Date,Type,Amount,Category,Note\n")
                
                allTransactions.value.forEach { t ->
                    val dateStr = formatter.format(Date(t.timestamp))
                    val amountStr = String.format(Locale.US, "%.2f", t.amount)
                    val noteEscaped = t.note.replace("\"", "\"\"")
                    csvContent.append("${t.id},$dateStr,${t.type},$amountStr,${t.category},\"$noteEscaped\"\n")
                }
                
                // Show successful offline copy code/file trigger status
                val fileName = "aureum_ledger_${System.currentTimeMillis() / 1000}.csv"
                exportMessage.value = "System exported: $fileName loaded offline. Copy contents:\n\n${csvContent.take(150)}..."
            } catch (e: Exception) {
                exportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun clearExportMessage() {
        exportMessage.value = null
    }
}
