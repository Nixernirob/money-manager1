package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.repository.AureumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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
    lateinit var allSoftLoanProfiles: StateFlow<List<SoftLoanProfile>>
    lateinit var allFormalLenders: StateFlow<List<FormalLender>>

    // Flow matching the active UI screen state
    val activeTab = MutableStateFlow("HOME") // HOME, TRANSACTIONS, STATS, ACCOUNTS, MORE

    // Security & Lock settings
    val isPinGuardEnabled = MutableStateFlow(prefs.getBoolean("pin_guard_enabled", false)) // Whether PIN guard is active
    val isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false)) // Whether Biometric/Fingerprint guard is active
    val isLocked = MutableStateFlow(false) // Whether App is CURRENTLY locked
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
    val transactionFilterType = MutableStateFlow("ALL") // "ALL", "EXPENSES", "INCOMES"
    val transactionCategoryFilter = MutableStateFlow("ALL") // Category names
    val transactionTimeFilter = MutableStateFlow("ALL") // "ALL", "TODAY", "7_DAYS", "15_DAYS", "30_DAYS", "THIS_MONTH", "CUSTOM_MONTH"
    val transactionCustomMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) // 0-11
    val transactionCustomYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))

    // Calculator state
    val calcExpression = MutableStateFlow("")
    val calcResult = MutableStateFlow("")

    // Export confirmation message state
    val exportMessage = MutableStateFlow<String?>(null)

    // Transaction being edited (null if adding a new transaction)
    val editingTransaction = MutableStateFlow<com.example.data.db.Transaction?>(null)

    init {
        if (isPinGuardEnabled.value || isBiometricEnabled.value) {
            isLocked.value = true
        }

        val database = AppDatabase.getDatabase(application)
        repository = AureumRepository(
            database.accountDao(),
            database.transactionDao(),
            database.loanDao(),
            database.categoryDao(),
            database.softLoanProfileDao(),
            database.formalLenderDao()
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

        allSoftLoanProfiles = repository.allSoftLoanProfiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allFormalLenders = repository.allFormalLenders.stateIn(
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

        // Trigger one-time v1.11 data reset for development
        if (!prefs.getBoolean("v111_data_reset_done_v111", false)) {
            viewModelScope.launch {
                repository.clearTransactions()
                prefs.edit().putBoolean("v111_data_reset_done_v111", true).apply()
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

        // Seed structured loans (Formal EMI + Soft friends loans)
        val now = System.currentTimeMillis()
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

    fun editTransaction(
        oldTransaction: Transaction,
        type: String,
        amount: Double,
        category: String,
        accountId: Int,
        toAccountId: Int?,
        note: String,
        timestamp: Long,
        receiptPath: String?,
        fee: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.deleteTransaction(oldTransaction)
            val trans = Transaction(
                type = type,
                amount = amount,
                category = category,
                accountId = accountId,
                toAccountId = toAccountId,
                note = note,
                timestamp = timestamp,
                receiptPath = receiptPath,
                fee = fee
            )
            repository.insertTransaction(trans)
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
            val count = allCategories.value.filter { it.type == type }.size
            repository.insertCategory(Category(name = name, icon = icon, type = type, isEditable = true, isDefault = false, displayOrder = count))
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

    fun moveCategoryUp(category: Category) {
        viewModelScope.launch {
            val list = allCategories.value.filter { it.type == category.type }.sortedBy { it.displayOrder }
            val index = list.indexOfFirst { it.id == category.id }
            if (index > 0) {
                val prev = list[index - 1]
                val prevOrder = prev.displayOrder
                val currentOrder = category.displayOrder
                repository.updateCategory(category.copy(displayOrder = prevOrder))
                repository.updateCategory(prev.copy(displayOrder = if (prevOrder == currentOrder) currentOrder + 1 else currentOrder))
            }
        }
    }

    fun moveCategoryDown(category: Category) {
        viewModelScope.launch {
            val list = allCategories.value.filter { it.type == category.type }.sortedBy { it.displayOrder }
            val index = list.indexOfFirst { it.id == category.id }
            if (index >= 0 && index < list.size - 1) {
                val next = list[index + 1]
                val nextOrder = next.displayOrder
                val currentOrder = category.displayOrder
                repository.updateCategory(category.copy(displayOrder = nextOrder))
                repository.updateCategory(next.copy(displayOrder = if (nextOrder == currentOrder) currentOrder - 1 else currentOrder))
            }
        }
    }

    suspend fun seedDefaultCategories() {
        val incomeDefaults = listOf(
            Category(name = "Salary", icon = "wallet", type = "INCOME", isEditable = true, isDefault = true, displayOrder = 0),
            Category(name = "Allowance", icon = "hand_coins", type = "INCOME", isEditable = true, isDefault = true, displayOrder = 1),
            Category(name = "Business Profit", icon = "trending_up", type = "INCOME", isEditable = true, isDefault = true, displayOrder = 2),
            Category(name = "Bonus", icon = "gift", type = "INCOME", isEditable = true, isDefault = true, displayOrder = 3),
            Category(name = "Other", icon = "circle", type = "INCOME", isEditable = false, isDefault = true, displayOrder = 4)
        )
        val expenseDefaults = listOf(
            Category(name = "Food", icon = "utensils", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 0),
            Category(name = "Apparel", icon = "shirt", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 1),
            Category(name = "House", icon = "home", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 2),
            Category(name = "Rent", icon = "building", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 3),
            Category(name = "Internet Bill", icon = "wifi", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 4),
            Category(name = "Transport", icon = "bus", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 5),
            Category(name = "Health", icon = "heart_pulse", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 6),
            Category(name = "Beauty", icon = "sparkles", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 7),
            Category(name = "Education", icon = "graduation_cap", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 8),
            Category(name = "Gift", icon = "gift", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 9),
            Category(name = "Entertainment", icon = "gamepad", type = "EXPENSE", isEditable = true, isDefault = true, displayOrder = 10),
            Category(name = "Other", icon = "circle", type = "EXPENSE", isEditable = false, isDefault = true, displayOrder = 11)
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
    fun addLoan(
        type: String,
        borrowerOrLender: String,
        amount: Double,
        interest: Double,
        emi: Double,
        direction: String,
        nextDue: Long,
        startDate: String = "",
        dueDateString: String = "",
        notes: String = "",
        status: String = "ACTIVE"
    ) {
        viewModelScope.launch {
            val l = Loan(
                type = type,
                borrowerOrLender = borrowerOrLender,
                loanAmount = amount,
                interestRate = interest,
                outstandingAmount = amount,
                nextDueDate = nextDue,
                emiAmount = emi,
                direction = direction,
                startDate = startDate,
                dueDateString = dueDateString,
                notes = notes,
                status = status,
                isCompleted = false
            )
            repository.insertLoan(l)
        }
    }

    fun repayLoanAmount(loan: Loan, amountPaid: Double) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val paymentEntry = "$amountPaid BDT ($dateStr)"
            val oldHistory = loan.paymentsJson
            val updatedHistory = if (oldHistory.isEmpty()) paymentEntry else "$oldHistory|$paymentEntry"
            
            val newOutstanding = maxOf(0.0, loan.outstandingAmount - amountPaid)
            val completed = newOutstanding <= 0.0
            val updatedStatus = if (completed) "PAID" else loan.status
            
            val updated = loan.copy(
                outstandingAmount = newOutstanding,
                isCompleted = completed,
                status = updatedStatus,
                paymentsJson = updatedHistory
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

    // --- Version 1.2 Soft Loan Profile and Formal Lender actions ---
    fun insertSoftLoanProfile(name: String, profilePic: String? = null, phoneNumber: String? = null, notes: String = "") {
        viewModelScope.launch {
            repository.insertSoftLoanProfile(
                SoftLoanProfile(name = name, profilePic = profilePic, phoneNumber = phoneNumber, notes = notes)
            )
        }
    }

    fun updateSoftLoanProfile(profile: SoftLoanProfile) {
        viewModelScope.launch {
            repository.updateSoftLoanProfile(profile)
        }
    }

    fun deleteSoftLoanProfile(profile: SoftLoanProfile) {
        viewModelScope.launch {
            repository.deleteSoftLoanProfile(profile)
        }
    }

    fun insertFormalLender(name: String, notes: String = "") {
        viewModelScope.launch {
            repository.insertFormalLender(
                FormalLender(name = name, notes = notes)
            )
        }
    }

    fun updateFormalLender(lender: FormalLender) {
        viewModelScope.launch {
            repository.updateFormalLender(lender)
        }
    }

    fun deleteFormalLender(lender: FormalLender) {
        viewModelScope.launch {
            repository.deleteFormalLender(lender)
        }
    }

    fun addSoftLoanTransaction(
        profileId: Int,
        type: String, // "BORROW", "GIVE", "REPAY", "RECEIVE"
        amount: Double,
        accountId: Int,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val txType = when (type) {
                "BORROW", "RECEIVE" -> "INCOME"
                "GIVE", "REPAY" -> "EXPENSE"
                else -> "EXPENSE"
            }
            val trans = Transaction(
                type = txType,
                amount = amount,
                category = "Soft Loan",
                accountId = accountId,
                toAccountId = null,
                note = note,
                timestamp = timestamp,
                softLoanProfileId = profileId,
                softLoanType = type,
                isLoanActivity = true
            )
            repository.insertTransaction(trans)
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

    fun unlockApp() {
        isLocked.value = false
        pinPromptMode.value = false
        pinInput.value = ""
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

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        isBiometricEnabled.value = enabled
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

    // --- Full Modular JSON Backup Engine ---
    suspend fun generateBackupJsonString(
        includeTransactions: Boolean,
        includeImages: Boolean,
        includeAccounts: Boolean,
        includeCategories: Boolean,
        includeLoans: Boolean
    ): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("currency", selectedCurrency.value)
        root.put("isDarkTheme", isDarkTheme.value)

        if (includeCategories) {
            val catArray = JSONArray()
            allCategories.value.forEach { cat ->
                val obj = JSONObject().apply {
                    put("name", cat.name)
                    put("icon", cat.icon)
                    put("type", cat.type)
                    put("isEditable", cat.isEditable)
                    put("isDefault", cat.isDefault)
                    put("displayOrder", cat.displayOrder)
                }
                catArray.put(obj)
            }
            root.put("categories", catArray)
        }

        if (includeAccounts) {
            val accArray = JSONArray()
            allAccounts.value.forEach { acc ->
                val obj = JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("balance", acc.balance)
                    put("type", acc.type)
                    if (acc.creditLimit != null) put("creditLimit", acc.creditLimit)
                    put("iconName", acc.iconName)
                    put("tintHex", acc.tintHex)
                }
                accArray.put(obj)
            }
            root.put("accounts", accArray)
        }

        if (includeLoans) {
            val loanArray = JSONArray()
            allLoans.value.forEach { loan ->
                val obj = JSONObject().apply {
                    put("id", loan.id)
                    put("type", loan.type)
                    put("borrowerOrLender", loan.borrowerOrLender)
                    put("loanAmount", loan.loanAmount)
                    put("interestRate", loan.interestRate)
                    put("outstandingAmount", loan.outstandingAmount)
                    put("nextDueDate", loan.nextDueDate)
                    put("emiAmount", loan.emiAmount)
                    put("direction", loan.direction)
                    put("isCompleted", loan.isCompleted)
                    put("startDate", loan.startDate)
                    put("dueDateString", loan.dueDateString)
                    put("notes", loan.notes)
                    put("status", loan.status)
                    put("paymentsJson", loan.paymentsJson)
                    if (loan.formalLenderId != null) put("formalLenderId", loan.formalLenderId)
                }
                loanArray.put(obj)
            }
            root.put("loans", loanArray)

            val profileArray = JSONArray()
            allSoftLoanProfiles.value.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    if (p.profilePic != null) put("profilePic", p.profilePic)
                    if (p.phoneNumber != null) put("phoneNumber", p.phoneNumber)
                    put("notes", p.notes)
                    put("createdAt", p.createdAt)
                }
                profileArray.put(obj)
            }
            root.put("softLoanProfiles", profileArray)

            val lenderArray = JSONArray()
            allFormalLenders.value.forEach { l ->
                val obj = JSONObject().apply {
                    put("id", l.id)
                    put("name", l.name)
                    put("notes", l.notes)
                    put("createdAt", l.createdAt)
                }
                lenderArray.put(obj)
            }
            root.put("formalLenders", lenderArray)
        }

        if (includeTransactions) {
            val txArray = JSONArray()
            allTransactions.value.forEach { tx ->
                val obj = JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("accountId", tx.accountId)
                    if (tx.toAccountId != null) put("toAccountId", tx.toAccountId)
                    put("note", tx.note)
                    put("timestamp", tx.timestamp)
                    put("isRecurring", tx.isRecurring)
                    put("fee", tx.fee)
                    if (tx.softLoanProfileId != null) put("softLoanProfileId", tx.softLoanProfileId)
                    if (tx.softLoanType != null) put("softLoanType", tx.softLoanType)
                    if (tx.formalLenderId != null) put("formalLenderId", tx.formalLenderId)
                    put("isLoanActivity", tx.isLoanActivity)

                    if (includeImages && !tx.receiptPath.isNullOrBlank()) {
                        try {
                            val file = File(tx.receiptPath)
                            if (file.exists()) {
                                val bytes = file.readBytes()
                                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                put("receiptBase64", base64)
                                put("receiptFileName", file.name)
                            }
                        } catch (_: Exception) {}
                    }
                }
                txArray.put(obj)
            }
            root.put("transactions", txArray)
        }

        root.toString(2)
    }

    suspend fun importBackupFromJsonString(context: Context, jsonString: String): String = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var restoredAccounts = 0
            var restoredTransactions = 0
            var restoredCategories = 0
            var restoredLoans = 0
            var restoredProfiles = 0

            if (root.has("currency")) {
                val curr = root.optString("currency", "৳")
                selectedCurrency.value = curr
            }

            // Restore Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                if (catArray.length() > 0) {
                    repository.clearAllCategories()
                    for (i in 0 until catArray.length()) {
                        val obj = catArray.getJSONObject(i)
                        val cat = Category(
                            name = obj.getString("name"),
                            icon = obj.optString("icon", "circle"),
                            type = obj.optString("type", "EXPENSE"),
                            isEditable = obj.optBoolean("isEditable", true),
                            isDefault = obj.optBoolean("isDefault", false),
                            displayOrder = obj.optInt("displayOrder", i)
                        )
                        repository.insertCategory(cat)
                        restoredCategories++
                    }
                }
            }

            // Restore Accounts
            if (root.has("accounts")) {
                val accArray = root.getJSONArray("accounts")
                if (accArray.length() > 0) {
                    repository.clearAccounts()
                    for (i in 0 until accArray.length()) {
                        val obj = accArray.getJSONObject(i)
                        val acc = Account(
                            id = obj.optInt("id", 0),
                            name = obj.getString("name"),
                            balance = obj.getDouble("balance"),
                            type = obj.optString("type", "CASH"),
                            creditLimit = if (obj.has("creditLimit")) obj.getDouble("creditLimit") else null,
                            iconName = obj.optString("iconName", "savings"),
                            tintHex = obj.optString("tintHex", "#bbc4f2")
                        )
                        repository.insertAccount(acc)
                        restoredAccounts++
                    }
                }
            }

            // Restore Loans
            if (root.has("loans")) {
                val loanArray = root.getJSONArray("loans")
                repository.clearLoans()
                for (i in 0 until loanArray.length()) {
                    val obj = loanArray.getJSONObject(i)
                    val loan = Loan(
                        id = obj.optInt("id", 0),
                        type = obj.optString("type", "FORMAL"),
                        borrowerOrLender = obj.optString("borrowerOrLender", "Facility"),
                        loanAmount = obj.optDouble("loanAmount", 0.0),
                        interestRate = obj.optDouble("interestRate", 0.0),
                        outstandingAmount = obj.optDouble("outstandingAmount", 0.0),
                        nextDueDate = obj.optLong("nextDueDate", System.currentTimeMillis()),
                        emiAmount = obj.optDouble("emiAmount", 0.0),
                        direction = obj.optString("direction", "BORROWED"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        startDate = obj.optString("startDate", ""),
                        dueDateString = obj.optString("dueDateString", ""),
                        notes = obj.optString("notes", ""),
                        status = obj.optString("status", "ACTIVE"),
                        paymentsJson = obj.optString("paymentsJson", ""),
                        formalLenderId = if (obj.has("formalLenderId")) obj.getInt("formalLenderId") else null
                    )
                    repository.insertLoan(loan)
                    restoredLoans++
                }
            }

            if (root.has("softLoanProfiles")) {
                val profileArray = root.getJSONArray("softLoanProfiles")
                repository.clearSoftLoanProfiles()
                for (i in 0 until profileArray.length()) {
                    val obj = profileArray.getJSONObject(i)
                    val p = SoftLoanProfile(
                        id = obj.optInt("id", 0),
                        name = obj.getString("name"),
                        profilePic = if (obj.has("profilePic")) obj.getString("profilePic") else null,
                        phoneNumber = if (obj.has("phoneNumber")) obj.getString("phoneNumber") else null,
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    repository.insertSoftLoanProfile(p)
                    restoredProfiles++
                }
            }

            if (root.has("formalLenders")) {
                val lenderArray = root.getJSONArray("formalLenders")
                repository.clearFormalLenders()
                for (i in 0 until lenderArray.length()) {
                    val obj = lenderArray.getJSONObject(i)
                    val l = FormalLender(
                        id = obj.optInt("id", 0),
                        name = obj.getString("name"),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    repository.insertFormalLender(l)
                }
            }

            // Restore Transactions
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                repository.clearTransactions()
                val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }

                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    var savedReceiptPath: String? = null

                    if (obj.has("receiptBase64")) {
                        try {
                            val b64 = obj.getString("receiptBase64")
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            val fileName = obj.optString("receiptFileName", "receipt_${System.currentTimeMillis()}_$i.jpg")
                            val destFile = File(receiptsDir, fileName)
                            destFile.writeBytes(bytes)
                            savedReceiptPath = destFile.absolutePath
                        } catch (_: Exception) {}
                    }

                    val trans = Transaction(
                        id = obj.optInt("id", 0),
                        type = obj.getString("type"),
                        amount = obj.getDouble("amount"),
                        category = obj.getString("category"),
                        accountId = obj.getInt("accountId"),
                        toAccountId = if (obj.has("toAccountId")) obj.getInt("toAccountId") else null,
                        note = obj.optString("note", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        receiptPath = savedReceiptPath,
                        isRecurring = obj.optBoolean("isRecurring", false),
                        fee = obj.optDouble("fee", 0.0),
                        softLoanProfileId = if (obj.has("softLoanProfileId")) obj.getInt("softLoanProfileId") else null,
                        softLoanType = if (obj.has("softLoanType")) obj.getString("softLoanType") else null,
                        formalLenderId = if (obj.has("formalLenderId")) obj.getInt("formalLenderId") else null,
                        isLoanActivity = obj.optBoolean("isLoanActivity", false)
                    )
                    val database = AppDatabase.getDatabase(context)
                    database.transactionDao().insertTransaction(trans)
                    restoredTransactions++
                }
            }

            "Backup restored successfully!\n\n• $restoredAccounts Vaults\n• $restoredTransactions Transactions\n• $restoredCategories Categories\n• $restoredLoans Facilities & $restoredProfiles Profiles"
        } catch (e: Exception) {
            "Failed to import backup: ${e.localizedMessage ?: "Corrupted file format"}"
        }
    }

    // Helper to write JSON string to a Uri (Storage Access Framework)
    fun writeJsonToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Helper to read string from a Uri (Storage Access Framework)
    fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun exportLedgerToCsv() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csv = StringBuilder()
        csv.append("ID,Type,Amount,Category,Account,ToAccount,Date,Note,Fee,IsLoan\n")
        allTransactions.value.forEach { tx ->
            val accName = allAccounts.value.find { it.id == tx.accountId }?.name ?: "Unknown"
            val toAccName = if (tx.toAccountId != null) allAccounts.value.find { it.id == tx.toAccountId }?.name ?: "" else ""
            val cleanNote = (tx.note ?: "").replace(",", " ").replace("\n", " ")
            val formattedDate = sdf.format(Date(tx.timestamp))
            csv.append("${tx.id},${tx.type},${tx.amount},${tx.category},\"$accName\",\"$toAccName\",$formattedDate,\"$cleanNote\",${tx.fee},${tx.isLoanActivity}\n")
        }
        exportMessage.value = csv.toString()
    }

    fun clearExportMessage() {
        exportMessage.value = null
    }
}
