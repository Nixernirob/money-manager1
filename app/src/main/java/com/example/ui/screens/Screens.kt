package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.Account
import com.example.data.db.Loan
import com.example.data.db.Transaction
import com.example.data.db.Category
import com.example.data.db.SoftLoanProfile
import com.example.data.db.FormalLender
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AureumViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import coil.compose.AsyncImage

private fun copyUriToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private val TextSecondary: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFC7C6CD) else Color(0xFF74777F)

private val MidnightPrimary: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFC1C5DC) else Color(0xFF005AC1)

private val MidnightSecondary: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFBDC5E7) else Color(0xFF74777F)

private val MidnightTertiary: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFBBC4F2) else Color(0xFFD1E1FF)

@Composable
fun DashboardScreen(
    viewModel: AureumViewModel,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val loans by viewModel.allLoans.collectAsState()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val softLoanProfiles by viewModel.allSoftLoanProfiles.collectAsState()

    var selectedDetailVault by remember { mutableStateOf<Account?>(null) }

    // Calculate Soft Loan Payable & Receivable across all profiles
    val softLoanTransactions = transactions.filter { it.softLoanProfileId != null }
    var totalSoftPayable = 0.0
    var totalSoftReceivable = 0.0

    softLoanProfiles.forEach { profile ->
        val profileTx = softLoanTransactions.filter { it.softLoanProfileId == profile.id }
        val borrowed = profileTx.filter { it.softLoanType == "BORROW" }.sumOf { it.amount }
        val given = profileTx.filter { it.softLoanType == "GIVE" }.sumOf { it.amount }
        val repaid = profileTx.filter { it.softLoanType == "REPAY" }.sumOf { it.amount }
        val received = profileTx.filter { it.softLoanType == "RECEIVE" }.sumOf { it.amount }

        val net = (given + repaid) - (borrowed + received)
        if (net < 0) {
            totalSoftPayable += -net
        } else {
            totalSoftReceivable += net
        }
    }

    // Calculate total net worth incorporating Formal & Soft Loans
    val accountsNetWorth = accounts.sumOf { 
        if (it.type == "CREDIT") -it.balance else it.balance 
    }
    val formalLentLoans = loans.filter { it.type == "FORMAL" && it.direction == "LENT" }.sumOf { it.outstandingAmount }
    val formalBorrowedLoans = loans.filter { it.type == "FORMAL" && it.direction == "BORROWED" }.sumOf { it.outstandingAmount }
    val totalNetWorth = accountsNetWorth + totalSoftReceivable - totalSoftPayable + formalLentLoans - formalBorrowedLoans

    val specs = AureumThemeStyles.specs
    val titleTextColor = specs.contentSecondary
    val balanceTextColor = specs.contentPrimary
    val iconTintColor = specs.activeContainer
    val buttonBg = specs.activeContainer
    val buttonContentColor = specs.onActiveContainer

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // A. Top Hero Layer (Balance)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .aureumNeonGlow()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL NET WORTH",
                        color = titleTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable { viewModel.isBalanceHidden.value = !isBalanceHidden }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isBalanceHidden) "••••••" else "$currency${String.format(Locale.getDefault(), "%,.2f", totalNetWorth)}",
                            color = balanceTextColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        AureumVectorIcon(
                            name = if (isBalanceHidden) "visibility_off" else "visibility",
                            tint = iconTintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onAddTransactionClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBg,
                                contentColor = buttonContentColor
                            )
                        ) {
                            AureumVectorIcon(name = "add", tint = buttonContentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transaction", color = buttonContentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // B. Account Carousel Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ACCOUNTS & CARDS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Prepend Total overview
                    AccountSwipeCard(
                        name = "Assets Overview",
                        balance = totalNetWorth,
                        type = "OVERVIEW",
                        tintColor = MidnightPrimary,
                        numberMask = "ALL WEALTH",
                        isHidden = isBalanceHidden,
                        currency = currency
                    )

                    accounts.forEach { acc ->
                        AccountSwipeCard(
                            name = acc.name,
                            balance = acc.balance,
                            type = acc.type,
                            tintColor = Color(android.graphics.Color.parseColor(acc.tintHex)),
                            numberMask = "•••• ${acc.id + 4239}",
                            isHidden = isBalanceHidden,
                            currency = currency,
                            onClick = { selectedDetailVault = acc }
                        )
                    }
                }
            }
        }

        // C. Quick Insights Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "QUICK PORTFOLIO INSIGHTS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val todaySpent = transactions.filter { 
                        it.type == "EXPENSE" && it.timestamp > System.currentTimeMillis() - 86400000 
                    }.sumOf { it.amount }

                    val monthlyExpense = transactions.filter {
                        it.type == "EXPENSE" && it.timestamp > System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                    }.sumOf { it.amount }

                    InsightChip(
                        label = "Spent Today",
                        value = "$currency${todaySpent.toInt()}",
                        accentColor = ExpenseCoral,
                        modifier = Modifier.weight(1f)
                    )
                    InsightChip(
                        label = "Monthly Spent",
                        value = "$currency${monthlyExpense.toInt()}",
                        accentColor = specs.activeContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // C.2 Soft Loan Position Cards (Pending to Pay / Pending to Receive)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SOFT LOAN POSITION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InsightChip(
                        label = "Pending to Pay",
                        value = if (isBalanceHidden) "••••" else "$currency${totalSoftPayable.toInt()}",
                        accentColor = ExpenseCoral,
                        modifier = Modifier.weight(1f)
                    )
                    InsightChip(
                        label = "Pending to Receive",
                        value = if (isBalanceHidden) "••••" else "$currency${totalSoftReceivable.toInt()}",
                        accentColor = AssetGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // D. Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT LEDGER ACTIVITY",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "See All",
                    color = MidnightPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.activeTab.value = "TRANSACTIONS" }
                )
            }
        }

        val limitTransList = transactions.take(4)
        if (limitTransList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found.", color = TextSecondary)
                }
            }
        } else {
            items(limitTransList) { trans ->
                TransactionRow(
                    transaction = trans,
                    currency = currency,
                    viewModel = viewModel,
                    onDelete = { viewModel.deleteTransaction(trans) }
                )
            }
        }
    }

    // Vault/Account Detail Modal (Slide-up style)
    if (selectedDetailVault != null) {
        val account = selectedDetailVault!!
        
        AlertDialog(
            onDismissRequest = { selectedDetailVault = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(account.name, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Vault Balance: $currency${String.format(Locale.getDefault(), "%,.2f", account.balance)}",
                            color = specs.contentSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = { selectedDetailVault = null }) {
                        Text("✕", color = specs.contentPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                var filterType by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE, TRANSFER
                
                val filteredVaultTransactions = transactions.filter {
                    (it.accountId == account.id || it.toAccountId == account.id) &&
                    (filterType == "ALL" || it.type == filterType)
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Filters row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL", "INCOME", "EXPENSE", "TRANSFER").forEach { fType ->
                            val active = filterType == fType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) specs.activeContainer else specs.inactiveContainer)
                                    .clickable { filterType = fType }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(fType) {
                                        "ALL" -> "All"
                                        "INCOME" -> "Income"
                                        "EXPENSE" -> "Expense"
                                        else -> "Transfer"
                                    },
                                    color = if (active) specs.onActiveContainer else specs.contentPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "TRANSACTIONS FOR THIS VAULT",
                        color = specs.contentSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    if (filteredVaultTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No matching transactions in this vault.", color = specs.contentSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(filteredVaultTransactions) { tr ->
                                TransactionRow(
                                    transaction = tr,
                                    currency = currency,
                                    viewModel = viewModel,
                                    onDelete = { viewModel.deleteTransaction(tr) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun AccountSwipeCard(
    name: String,
    balance: Double,
    type: String,
    tintColor: Color,
    numberMask: String,
    isHidden: Boolean,
    currency: String,
    onClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Choose professional backgrounds matching styling instructions
    val cardBackgroundColors = if (isDark) {
        listOf(tintColor.copy(alpha = 0.2f), tintColor.copy(alpha = 0.04f))
    } else {
        when (type.uppercase()) {
            "BANK" -> listOf(Color(0xFFD1E1FF), Color(0xFFD1E1FF).copy(alpha = 0.8f))
            "CREDIT" -> listOf(Color(0xFFFFDADA), Color(0xFFFFDADA).copy(alpha = 0.8f))
            else -> listOf(tintColor.copy(alpha = 0.15f), tintColor.copy(alpha = 0.10f))
        }
    }
    
    val cardBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        Color(0xFFE0E2EC)
    }

    val primaryTextColor = if (isDark) {
        Color.White
    } else {
        when (type.uppercase()) {
            "BANK" -> Color(0xFF00305F)
            "CREDIT" -> Color(0xFF410002)
            else -> Color(0xFF1A1C1E)
        }
    }

    val secondaryTextColor = if (isDark) {
        TextSecondary
    } else {
        Color(0xFF74777F)
    }

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(
                Brush.linearGradient(
                    colors = cardBackgroundColors
                )
            )
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    color = primaryTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AureumVectorIcon(
                    name = when (type.uppercase()) {
                        "BANK", "ACCOUNTS" -> "bank"
                        "WALLET", "CASH" -> "vault"
                        "CREDIT", "CARD" -> "credit_card"
                        else -> "stats"
                    },
                    tint = if (isDark) tintColor else primaryTextColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = numberMask,
                    color = secondaryTextColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isHidden) "••••••" else "$currency${String.format(Locale.getDefault(), "%,.2f", balance)}",
                        color = primaryTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun InsightChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val normalizedLabel = label.lowercase()
    
    val (backgroundColor, borderColor, titleColor, valColor) = if (isDark) {
        val bg = Color.White.copy(alpha = 0.03f)
        val border = Color.White.copy(alpha = 0.07f)
        val tc = TextSecondary
        val vc = accentColor
        FourColors(bg, border, tc, vc)
    } else {
        when {
            normalizedLabel.contains("spent") || normalizedLabel.contains("expense") -> {
                FourColors(Color(0xFFDEE1F9), Color(0xFFC5CAE9), Color(0xFF161B33).copy(alpha = 0.62f), Color(0xFF161B33))
            }
            normalizedLabel.contains("remain") || normalizedLabel.contains("safe") || normalizedLabel.contains("balance") -> {
                FourColors(Color(0xFFD8E6FF), Color(0xFFBBDEFB), Color(0xFF001D36).copy(alpha = 0.62f), Color(0xFF001D36))
            }
            normalizedLabel.contains("loan") || normalizedLabel.contains("due") || normalizedLabel.contains("debt") -> {
                FourColors(Color(0xFFFFE08D), Color(0xFFFFD54F), Color(0xFF241A00).copy(alpha = 0.62f), Color(0xFF241A00))
            }
            else -> {
                FourColors(Color(0xFFF0F0F7), Color(0xFFE0E2EC), Color(0xFF44474E).copy(alpha = 0.8f), Color(0xFF1A1C1E))
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(), 
                color = titleColor, 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value, 
                color = valColor, 
                fontSize = 15.sp, 
                fontWeight = FontWeight.Bold, 
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private data class FourColors(val bg: Color, val border: Color, val text: Color, val valueColor: Color)

@Composable
fun TransactionRow(
    transaction: Transaction,
    currency: String,
    viewModel: AureumViewModel,
    onDelete: () -> Unit
) {
    var showDetailPopup by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val specs = AureumThemeStyles.specs

    val categories by viewModel.allCategories.collectAsState()
    val categoryIcon = categories.find { it.name.lowercase() == transaction.category.lowercase() }?.icon

    val rowBackground = if (isDark) Color.White.copy(alpha = 0.03f) else Color.White
    val rowBorderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE0E2EC)
    val noteColor = if (isDark) Color.White else Color(0xFF1A1C1E)
    val categoryColor = if (isDark) TextSecondary else Color(0xFF74777F)
    val amountColor = when (transaction.type) {
        "INCOME" -> if (isDark) AssetGreen else Color(0xFF2E7D32)
        "TRANSFER" -> if (isDark) specs.activeContainer else Color(0xFF005AC1)
        else -> if (isDark) Color.White else Color(0xFF1A1C1E)
    }
    
    val avatarBg = when (transaction.type) {
        "INCOME" -> if (isDark) AssetGreen.copy(alpha = 0.15f) else Color(0xFFE8F3D6)
        "TRANSFER" -> if (isDark) specs.activeContainer.copy(alpha = 0.15f) else Color(0xFFE5F1FF)
        else -> if (isDark) ExpenseCoral.copy(alpha = 0.15f) else Color(0xFFF1F1FB)
    }
    
    val avatarIconTint = when (transaction.type) {
        "INCOME" -> if (isDark) AssetGreen else Color(0xFF2D3325)
        "TRANSFER" -> if (isDark) specs.activeContainer else Color(0xFF005AC1)
        else -> if (isDark) ExpenseCoral else Color(0xFF44474E)
    }

    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE0E2EC)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(rowBackground)
            .border(1.dp, rowBorderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { showDetailPopup = true }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    AureumVectorIcon(
                        name = categoryIcon ?: when (transaction.category.lowercase()) {
                            "housing", "house" -> "home"
                            "retail", "shopping" -> "retail"
                            "dining", "food" -> "dining"
                            "salary" -> "income"
                            "internal", "loans" -> "transfer"
                            else -> {
                                if (transaction.type == "TRANSFER") "transfer" else "expense"
                            }
                        },
                        tint = avatarIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.note.ifBlank { "Untitled ${transaction.category}" },
                        color = noteColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.softLoanProfileId != null) {
                        val profiles by viewModel.allSoftLoanProfiles.collectAsState()
                        val pName = profiles.find { it.id == transaction.softLoanProfileId }?.name ?: "Unknown Person"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(specs.activeContainer.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Soft Loan",
                                    color = specs.activeContainer,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (specs.isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = transaction.softLoanType ?: "Activity",
                                    color = if (specs.isDark) Color.White else Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "• $pName",
                                color = categoryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (transaction.category == "Loans" || transaction.category == "Formal Loan" || transaction.isLoanActivity) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ExpenseCoral.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Formal Loan",
                                    color = ExpenseCoral,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (transaction.type == "TRANSFER") "Transfer" else transaction.category,
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (transaction.type == "TRANSFER" && transaction.fee > 0.0) {
                        Text(
                            text = "Fee: $currency${String.format(Locale.getDefault(), "%,.2f", transaction.fee)}",
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (transaction.type == "TRANSFER") {
                        "⇄ $currency${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}"
                    } else {
                        "${if (transaction.type == "INCOME") "+" else "-"}$currency${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}"
                    },
                    color = amountColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                    color = categoryColor,
                    fontSize = 10.sp
                )
            }
        }
    }

    // Modal Details Dialog
    if (showDetailPopup) {
        val accounts by viewModel.allAccounts.collectAsState()
        val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown Account"
        val toAccountName = if (transaction.toAccountId != null) {
            accounts.find { it.id == transaction.toAccountId }?.name
        } else null

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDetailPopup = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var animateIn by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                animateIn = true
            }
            val popupScale by animateFloatAsState(
                targetValue = if (animateIn) 1f else 0.85f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "detail_scale"
            )
            val popupAlpha by animateFloatAsState(
                targetValue = if (animateIn) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "detail_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .graphicsLayer {
                        scaleX = popupScale
                        scaleY = popupScale
                        this.alpha = popupAlpha
                    }
                    .aureumGlass(shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSACTION DETAILS",
                            color = specs.contentSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        IconButton(onClick = { showDetailPopup = false }) {
                            Text("✕", color = specs.contentPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

                    // Name / Note
                    Text(
                        text = transaction.note.ifBlank { "Untitled ${transaction.category}" },
                        color = specs.contentPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Amount
                    Text(
                        text = if (transaction.type == "TRANSFER") {
                            "⇄ $currency${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}"
                        } else {
                            "${if (transaction.type == "INCOME") "+" else "-"}$currency${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}"
                        },
                        color = amountColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Meta Info List
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                AureumVectorIcon(
                                    name = categoryIcon ?: when (transaction.category.lowercase()) {
                                        "housing", "house" -> "home"
                                        "retail", "shopping" -> "retail"
                                        "dining", "food" -> "dining"
                                        "salary" -> "income"
                                        "internal", "loans" -> "transfer"
                                        else -> if (transaction.type == "TRANSFER") "transfer" else "expense"
                                    },
                                    tint = avatarIconTint,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "Category: ${transaction.category}",
                                color = specs.contentPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (transaction.softLoanProfileId != null) {
                            val profiles by viewModel.allSoftLoanProfiles.collectAsState()
                            val pName = profiles.find { it.id == transaction.softLoanProfileId }?.name ?: "Unknown Person"
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("👤", fontSize = 14.sp)
                                Text(
                                    text = "Relationship Profile: $pName (${transaction.softLoanType})",
                                    color = specs.contentPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📅", fontSize = 14.sp)
                            Text(
                                text = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                                color = specs.contentSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💼", fontSize = 14.sp)
                            Text(
                                text = if (transaction.type == "TRANSFER") {
                                    "From $accountName to ${toAccountName ?: "Unknown"}"
                                } else {
                                    "Vault: $accountName"
                                },
                                color = specs.contentSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Attachment Preview
                    if (transaction.receiptPath != null) {
                        Text(
                            text = "ATTACHMENT PREVIEW",
                            color = specs.contentSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                                .border(1.dp, rowBorderColor, RoundedCornerShape(12.dp))
                                .clickable { showFullImage = true },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = transaction.receiptPath,
                                contentDescription = "Receipt Attachment Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔍 Click to Expand", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Bottom Row: Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Edit Button
                        Button(
                            onClick = {
                                viewModel.editingTransaction.value = transaction
                                showDetailPopup = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit", color = specs.onActiveContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Delete Button
                        Button(
                            onClick = {
                                onDelete()
                                showDetailPopup = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete", color = ExpenseCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal Receipt Fullscreen Image Viewer Dialog
    if (showFullImage && transaction.receiptPath != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullImage = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showFullImage = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = transaction.receiptPath,
                    contentDescription = "Receipt Expanded View",
                    modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TRANSACTIONS SCREEN
// -------------------------------------------------------------
@Composable
fun TransactionsScreen(
    viewModel: AureumViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val filterType by viewModel.transactionFilterType.collectAsState()
    val categoryFilter by viewModel.transactionCategoryFilter.collectAsState()

    // Aggregate category types for filter
    val categoriesList = listOf("ALL") + transactions.map { it.category }.distinct()

    val filteredList = transactions.filter {
        val categoryMatch = categoryFilter == "ALL" || it.category == categoryFilter
        val typeMatch = when (filterType) {
            "EXPENSES" -> it.type == "EXPENSE"
            "INCOMES" -> it.type == "INCOME"
            else -> true
        }
        categoryMatch && typeMatch
    }

    val specs = AureumThemeStyles.specs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "LEDGER ARCHIVE",
            color = specs.contentSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Type Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val activeBgColor = specs.activeContainer
            val inactiveBgColor = specs.inactiveContainer
            val inactiveBorderColor = specs.inactiveContainerBorder
            val activeTextColor = specs.onActiveContainer
            val inactiveTextColor = specs.contentPrimary

            listOf("ALL", "EXPENSES", "INCOMES").forEach { t ->
                val active = filterType == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) activeBgColor else inactiveBgColor)
                        .then(
                            if (!active) Modifier.border(1.dp, inactiveBorderColor, RoundedCornerShape(12.dp)) else Modifier
                        )
                        .clickable { viewModel.transactionFilterType.value = t }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t,
                        color = if (active) activeTextColor else inactiveTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Category Filter scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val activeBg = specs.activeContainer
            val inactiveBg = specs.inactiveContainer
            val inactiveBorder = specs.inactiveContainerBorder
            val activeText = specs.onActiveContainer
            val inactiveText = specs.contentPrimary

            categoriesList.forEach { cat ->
                val active = categoryFilter == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) activeBg else inactiveBg)
                        .border(1.dp, if (active) Color.Transparent else inactiveBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.transactionCategoryFilter.value = cat }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (active) activeText else inactiveText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Expense distribution Animated Pie Chart visualization
        ExpenseDistributionPieChart(transactions = transactions, viewModel = viewModel)

        // List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching items.", color = TextSecondary)
                    }
                }
            } else {
                items(filteredList) { trans ->
                    TransactionRow(
                        transaction = trans,
                        currency = currency,
                        viewModel = viewModel,
                        onDelete = { viewModel.deleteTransaction(trans) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseDistributionPieChart(transactions: List<Transaction>, viewModel: AureumViewModel) {
    val expenseTransactions = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }
    }
    
    val categoryTotals = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
    
    val totalExpense = remember(categoryTotals) {
        categoryTotals.sumOf { it.second }
    }

    val specs = AureumThemeStyles.specs
    val isDark = specs.isDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aureumGlass(shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "EXPENSE CATEGORY DISTRIBUTION",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (totalExpense <= 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No expense data recorded.", color = TextSecondary, fontSize = 12.sp)
            }
        } else {
            var animationTriggered by remember { mutableStateOf(false) }
            LaunchedEffect(key1 = categoryTotals) {
                animationTriggered = true
            }
            
            val sweepProgress by animateFloatAsState(
                targetValue = if (animationTriggered) 1f else 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "pie_sweep"
            )

            val colors = listOf(
                Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
                Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DD0E1),
                Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFFD54F)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier.size(110.dp)
                ) {
                    var startAngle = -90f
                    categoryTotals.forEachIndexed { index, (_, amount) ->
                        val sweepAngle = ((amount / totalExpense) * 360f).toFloat() * sweepProgress
                        val color = colors[index % colors.size]
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 130.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryTotals.forEachIndexed { index, (category, amount) ->
                        val percent = (amount / totalExpense) * 100
                        val color = colors[index % colors.size]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = "$category (${String.format(Locale.getDefault(), "%.1f", percent)}%)",
                                color = specs.contentPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ANALYTICS / STATS SCREEN
// -------------------------------------------------------------
@Composable
fun StatsScreen(
    viewModel: AureumViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    val totalExpenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalIncomes = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val specs = AureumThemeStyles.specs
    val isDark = specs.isDark

    val mPrimary = MidnightPrimary
    val mSecondary = MidnightSecondary
    val mTertiary = MidnightTertiary

    // Map spend distributions
    val categoriesSpend = remember(transactions, mPrimary, mSecondary, mTertiary) {
        val map = mutableMapOf<String, Double>()
        transactions.filter { it.type == "EXPENSE" }.forEach {
            map[it.category] = (map[it.category] ?: 0.0) + it.amount
        }
        val sortedList = map.toList().sortedByDescending { it.second }.take(3)
        val colors = listOf(mTertiary, mPrimary, mSecondary)
        
        sortedList.mapIndexed { idx, pair ->
            PieSegment(category = pair.first, value = pair.second, color = colors.getOrElse(idx) { mPrimary })
        }
    }

    val topCategory = categoriesSpend.firstOrNull()?.category ?: "None"

    // Mock trend curve values matching recent spends
    val trendValues = remember(transactions) {
        val raw = transactions.filter { it.type == "EXPENSE" }.take(7).map { it.amount.toFloat() }.reversed()
        if (raw.isEmpty()) listOf(0f, 0f, 0f) else raw
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "FINANCIAL PERFORMANCE",
            color = specs.contentSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Bento Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val outflowBg = if (isDark) ExpenseCoral.copy(alpha = 0.05f) else Color(0xFFFFECEB)
            val outflowBorder = if (isDark) ExpenseCoral.copy(alpha = 0.12f) else Color(0xFFFFDAD7)
            val inflowBg = if (isDark) AssetGreen.copy(alpha = 0.05f) else Color(0xFFE8F5E9)
            val inflowBorder = if (isDark) AssetGreen.copy(alpha = 0.12f) else Color(0xFFC8E6C9)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(outflowBg)
                    .border(1.dp, outflowBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Total Outflow", color = specs.contentSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$currency${String.format(Locale.getDefault(), "%,.0f", totalExpenses)}",
                        color = ExpenseCoral,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(inflowBg)
                    .border(1.dp, inflowBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Total Inputs", color = specs.contentSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$currency${String.format(Locale.getDefault(), "%,.0f", totalIncomes)}",
                        color = if (isDark) AssetGreen else Color(0xFF2E7D32),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Donut breakdown card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutChart(
                    segments = categoriesSpend,
                    modifier = Modifier.size(130.dp),
                    centerTextLabel = "DOMINANCE",
                    centerTextVal = topCategory
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("SPEND BREAKDOWN", color = specs.contentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    categoriesSpend.forEach { seg ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(seg.color))
                            Text("${seg.category}: $currency${seg.value.toInt()}", color = specs.contentSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Spending trend curve line
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "SPENDING TREND VECTOR",
                color = specs.contentSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            TrendLineChart(values = trendValues, accentColor = specs.activeContainer)
        }
    }
}

// -------------------------------------------------------------
// ACCOUNTS SCREEN
// -------------------------------------------------------------
@Composable
fun AccountsScreen(
    viewModel: AureumViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountNameInput by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var creditLimitInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ACCOUNTS") } // CASH, ACCOUNTS, CARD

    // Edit Account Dialog states
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountBalance by remember { mutableStateOf("") }
    var editAccountType by remember { mutableStateOf("ACCOUNTS") }
    var editCreditLimit by remember { mutableStateOf("") }

    val assetsTotal = accounts.filter { it.type != "CREDIT" && it.type != "CARD" }.sumOf { it.balance }
    val creditOutflow = accounts.filter { it.type == "CREDIT" || it.type == "CARD" }.sumOf { it.balance }
    val netWorthVal = assetsTotal - creditOutflow
    val specs = AureumThemeStyles.specs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BALANCE SHEETS Overview",
                color = specs.contentSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Button(
                onClick = { 
                    selectedType = "ACCOUNTS"
                    showAddAccountDialog = true 
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (specs.isDark) specs.activeContainer.copy(alpha = 0.25f) else specs.activeContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ Card/Acc", color = if (specs.isDark) specs.activeContainer else specs.onActiveContainer, fontWeight = FontWeight.Bold)
            }
        }

        // Net worth progress bar bento card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ASSETS VS LIABILITIES RATIO", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$currency${String.format(Locale.getDefault(), "%,.0f", assetsTotal)}", color = AssetGreen, fontWeight = FontWeight.Bold)
                    Text("$currency${String.format(Locale.getDefault(), "%,.0f", creditOutflow)}", color = ExpenseCoral, fontWeight = FontWeight.Bold)
                }

                // Progress Bar representation
                val ratio = if (assetsTotal + creditOutflow > 0) (assetsTotal / (assetsTotal + creditOutflow)).toFloat() else 1f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(specs.inactiveContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .background(specs.activeContainer)
                    )
                }
            }
        }

        // Full Account List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(accounts) { acc ->
                val cardColor = Color(android.graphics.Color.parseColor(acc.tintHex))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(specs.inactiveContainer)
                        .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(18.dp))
                        .clickable {
                            editingAccount = acc
                            editAccountName = acc.name
                            editAccountBalance = acc.balance.toString()
                            editAccountType = acc.type.uppercase()
                            editCreditLimit = acc.creditLimit?.toString() ?: ""
                            showEditAccountDialog = true
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(cardColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AureumVectorIcon(name = acc.iconName, tint = cardColor, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(acc.name, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(acc.type, color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "$currency${String.format(Locale.getDefault(), "%,.2f", acc.balance)}",
                            color = if (acc.type == "CREDIT" || acc.type == "CARD") ExpenseCoral else specs.contentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                        if (acc.creditLimit != null) {
                            Text("Limit: $currency${acc.creditLimit.toInt()}", color = specs.contentSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Add Account/Card Facility", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = accountNameInput,
                        onValueChange = { accountNameInput = it },
                        label = { Text("Name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    TextField(
                        value = balanceInput,
                        onValueChange = { balanceInput = it },
                        label = { Text("Starting Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    
                    Text("SELECT VAULT GROUP", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "ACCOUNTS", "CARD").forEach { t ->
                            val active = selectedType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) specs.activeContainer else specs.inactiveContainer)
                                    .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedType = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 10.sp,
                                    color = if (active) specs.onActiveContainer else specs.contentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (selectedType == "CARD" || selectedType == "CREDIT") {
                        TextField(
                            value = creditLimitInput,
                            onValueChange = { creditLimitInput = it },
                            label = { Text("Credit Card Limit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = specs.contentPrimary,
                                unfocusedTextColor = specs.contentPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = creditLimitInput.toDoubleOrNull()
                        viewModel.addAccount(
                            name = accountNameInput,
                            initialBalance = balanceInput.toDoubleOrNull() ?: 0.0,
                            type = selectedType,
                            creditLimit = limit,
                            tintHex = "#bbc4f2"
                        )
                        // Reset input fields
                        accountNameInput = ""
                        balanceInput = ""
                        creditLimitInput = ""
                        showAddAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Add", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Edit and Delete Account dialog
    if (showEditAccountDialog && editingAccount != null) {
        val targetAccount = editingAccount!!
        AlertDialog(
            onDismissRequest = { showEditAccountDialog = false },
            title = { Text("Edit or Delete Vault", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = editAccountName,
                        onValueChange = { editAccountName = it },
                        label = { Text("Name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    TextField(
                        value = editAccountBalance,
                        onValueChange = { editAccountBalance = it },
                        label = { Text("Current Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    
                    Text("SELECT VAULT GROUP", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "ACCOUNTS", "CARD").forEach { t ->
                            val active = editAccountType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) specs.activeContainer else specs.inactiveContainer)
                                    .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                                    .clickable { editAccountType = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    fontSize = 10.sp,
                                    color = if (active) specs.onActiveContainer else specs.contentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (editAccountType == "CARD" || editAccountType == "CREDIT") {
                        TextField(
                            value = editCreditLimit,
                            onValueChange = { editCreditLimit = it },
                            label = { Text("Credit Card Limit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = specs.contentPrimary,
                                unfocusedTextColor = specs.contentPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            viewModel.deleteAccount(targetAccount)
                            showEditAccountDialog = false
                            editingAccount = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DELETE VAULT", color = ExpenseCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = editCreditLimit.toDoubleOrNull()
                        val updated = targetAccount.copy(
                            name = editAccountName,
                            balance = editAccountBalance.toDoubleOrNull() ?: targetAccount.balance,
                            type = editAccountType,
                            creditLimit = limit,
                            iconName = when (editAccountType) {
                                "ACCOUNTS" -> "account_balance"
                                "CASH" -> "savings"
                                "CARD" -> "credit_card"
                                else -> "cash"
                            }
                        )
                        viewModel.updateAccount(updated)
                        showEditAccountDialog = false
                        editingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Save Changes", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEditAccountDialog = false 
                        editingAccount = null
                    }
                ) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

data class SoftLedgerSummary(
    val payable: Double,
    val receivable: Double,
    val net: Double,
    val txCount: Int,
    val lastDate: Long?
)

// -------------------------------------------------------------
// LOANS REPAYMENT TIMELINE & SOFT LOANS SYSTEM
// -------------------------------------------------------------
@Composable
fun LoansScreen(
    viewModel: AureumViewModel,
    modifier: Modifier = Modifier
) {
    val loans by viewModel.allLoans.collectAsState()
    val softProfiles by viewModel.allSoftLoanProfiles.collectAsState()
    val formalLenders by viewModel.allFormalLenders.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val specs = AureumThemeStyles.specs

    var showFormalLoansMode by remember { mutableStateOf(true) }
    
    // Dialog state toggles
    var showAddSoftProfileDialog by remember { mutableStateOf(false) }
    var showAddSoftTxDialog by remember { mutableStateOf(false) }
    var showAddFormalLenderDialog by remember { mutableStateOf(false) }
    var showAddFormalLoanDialog by remember { mutableStateOf(false) }

    // Detail States
    val selectedSoftProfileForDetail = remember { mutableStateOf<SoftLoanProfile?>(null) }
    val selectedFormalLenderForDetail = remember { mutableStateOf<FormalLender?>(null) }

    // --- FORMAL LOAN CALCULATIONS ---
    val activeFormalLoans = loans.count { it.type == "FORMAL" && !it.isCompleted && it.status == "ACTIVE" }
    val paidLoans = loans.count { it.type == "FORMAL" && it.isCompleted }
    val overdueLoans = loans.count { it.type == "FORMAL" && !it.isCompleted && it.status == "OVERDUE" }
    val totalFormalBorrowed = loans.filter { it.type == "FORMAL" && it.direction == "BORROWED" && !it.isCompleted }.sumOf { it.outstandingAmount }
    val totalFormalGiven = loans.filter { it.type == "FORMAL" && it.direction == "LENT" && !it.isCompleted }.sumOf { it.outstandingAmount }

    // --- SOFT LOAN CALCULATIONS ---
    val softLoanTransactions = transactions.filter { it.softLoanProfileId != null }
    var totalSoftPayableValue = 0.0
    var totalSoftReceivableValue = 0.0

    val profileLedgers = softProfiles.associateWith { profile ->
        val profileTx = softLoanTransactions.filter { it.softLoanProfileId == profile.id }
        val borrowed = profileTx.filter { it.softLoanType == "BORROW" }.sumOf { it.amount }
        val given = profileTx.filter { it.softLoanType == "GIVE" }.sumOf { it.amount }
        val repaid = profileTx.filter { it.softLoanType == "REPAY" }.sumOf { it.amount }
        val received = profileTx.filter { it.softLoanType == "RECEIVE" }.sumOf { it.amount }

        val net = (given + repaid) - (borrowed + received)
        val payable = if (net < 0) -net else 0.0
        val receivable = if (net > 0) net else 0.0
        
        SoftLedgerSummary(
            payable = payable,
            receivable = receivable,
            net = net,
            txCount = profileTx.size,
            lastDate = profileTx.maxOfOrNull { it.timestamp }
        )
    }

    profileLedgers.values.forEach { ledger ->
        totalSoftPayableValue += ledger.payable
        totalSoftReceivableValue += ledger.receivable
    }
    val netSoftPosition = totalSoftReceivableValue - totalSoftPayableValue

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOANS & CREDIT CHANNELS",
                color = specs.contentSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showFormalLoansMode) {
                    Button(
                        onClick = { showAddFormalLenderDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = specs.inactiveContainer),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Lender", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { showAddFormalLoanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Facility", color = specs.onActiveContainer, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                } else {
                    Button(
                        onClick = { showAddSoftProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = specs.inactiveContainer),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Profile", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { showAddSoftTxDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Record", color = specs.onActiveContainer, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        // --- DYNAMIC ANALYTICS DASHBOARD ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (showFormalLoansMode) "FORMAL FACILITIES OVERVIEW" else "SOFT RELATIONSHIP LEDGER",
                    color = specs.contentSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val payableTitle = if (showFormalLoansMode) "TOTAL BORROWED" else "PENDING TO PAY"
                    val payableVal = if (showFormalLoansMode) totalFormalBorrowed else totalSoftPayableValue
                    val receivableTitle = if (showFormalLoansMode) "TOTAL LENT" else "PENDING RECEIVABLE"
                    val receivableVal = if (showFormalLoansMode) totalFormalGiven else totalSoftReceivableValue

                    // Payable Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (specs.isDark) Color.White.copy(alpha = 0.03f) else specs.inactiveContainer)
                            .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.06f) else specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(payableTitle, color = specs.contentSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currency${String.format(Locale.getDefault(), "%,.0f", payableVal)}",
                            color = ExpenseCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    // Receivable Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (specs.isDark) Color.White.copy(alpha = 0.03f) else specs.inactiveContainer)
                            .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.06f) else specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(receivableTitle, color = specs.contentSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currency${String.format(Locale.getDefault(), "%,.0f", receivableVal)}",
                            color = AssetGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Sub-metrics / Health metrics
                if (showFormalLoansMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active Loans", color = specs.contentSecondary, fontSize = 8.sp)
                            Text("$activeFormalLoans", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(specs.inactiveContainerBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Fully Paid", color = specs.contentSecondary, fontSize = 8.sp)
                            Text("$paidLoans", color = AssetGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(specs.inactiveContainerBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Overdue Items", color = specs.contentSecondary, fontSize = 8.sp)
                            Text("$overdueLoans", color = ExpenseCoral, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NET SOFT POSITION", color = specs.contentSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (netSoftPosition >= 0.0) "Net Receivable" else "Net Payable",
                                color = if (netSoftPosition >= 0.0) AssetGreen else ExpenseCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$currency${String.format(Locale.getDefault(), "%,.0f", Math.abs(netSoftPosition))}",
                            color = if (netSoftPosition >= 0.0) AssetGreen else ExpenseCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- TAB SELECTOR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showFormalLoansMode = true },
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (!showFormalLoansMode) Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(100)) else Modifier
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showFormalLoansMode) specs.activeContainer else specs.inactiveContainer
                )
            ) {
                Text("FORMAL FACILITIES", color = if (showFormalLoansMode) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
                onClick = { showFormalLoansMode = false },
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (showFormalLoansMode) Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(100)) else Modifier
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showFormalLoansMode) specs.activeContainer else specs.inactiveContainer
                )
            ) {
                Text("SOFT CHANNELS", color = if (!showFormalLoansMode) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        // --- CONTENT SEGMENTS ---
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (showFormalLoansMode) {
                // Formal Lenders horizontal row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("INSTITUTIONS & LENDERS", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (formalLenders.isEmpty()) {
                            Text("No lenders registered yet. Add one to associate formal loans.", color = specs.contentSecondary, fontSize = 11.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                formalLenders.forEach { lender ->
                                    val lenderLoans = loans.filter { it.type == "FORMAL" && it.borrowerOrLender == lender.name }
                                    val outstanding = lenderLoans.sumOf { it.outstandingAmount }
                                    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .framerHoverEffect(interactionSource)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (specs.isDark) Color.White.copy(alpha = 0.03f) else specs.inactiveContainer)
                                            .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.06f) else specs.inactiveContainerBorder, RoundedCornerShape(14.dp))
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = androidx.compose.foundation.LocalIndication.current,
                                                onClick = { selectedFormalLenderForDetail.value = lender }
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(lender.name, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${lenderLoans.size} loans • O/S: $currency${outstanding.toInt()}", color = specs.contentSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Formal Facilities List
                val formalLoans = loans.filter { it.type == "FORMAL" }
                if (formalLoans.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No formal loans active.", color = specs.contentSecondary)
                        }
                    }
                } else {
                    items(formalLoans) { loan ->
                        FormalRepayTimelineCard(loan = loan, currency = currency) { payAmt ->
                            viewModel.repayLoanAmount(loan, payAmt)
                        }
                    }
                }
            } else {
                // Soft Channels Profiles List
                if (softProfiles.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No relationship profiles found. Please create one to begin logging soft loans.", color = specs.contentSecondary)
                        }
                    }
                } else {
                    items(softProfiles) { profile ->
                        val ledger = profileLedgers[profile]
                        val payable = ledger?.payable ?: 0.0
                        val receivable = ledger?.receivable ?: 0.0
                        val net = ledger?.net ?: 0.0

                        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .framerHoverEffect(interactionSource)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (specs.isDark) Color.White.copy(alpha = 0.03f) else specs.inactiveContainer)
                                .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.06f) else specs.inactiveContainerBorder, RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    onClick = { selectedSoftProfileForDetail.value = profile }
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colored Initial Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (net > 0.0) AssetGreen.copy(alpha = 0.15f)
                                                else if (net < 0.0) ExpenseCoral.copy(alpha = 0.15f)
                                                else specs.activeContainer.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profile.name.take(2).uppercase(),
                                            color = if (net > 0.0) AssetGreen else if (net < 0.0) ExpenseCoral else specs.contentPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Column {
                                        Text(profile.name, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            text = if (net > 0.0) "Owes you $currency${receivable.toInt()}"
                                                   else if (net < 0.0) "You owe them $currency${payable.toInt()}"
                                                   else "Fully Settled",
                                            color = if (net > 0.0) AssetGreen else if (net < 0.0) ExpenseCoral else specs.contentSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Quick Actions
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            showAddSoftTxDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("+ Transact", color = specs.onActiveContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD SOFT PROFILE DIALOG ---
    if (showAddSoftProfileDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var profileNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSoftProfileDialog = false },
            title = { Text("Create Soft Profile", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Person Name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone (optional)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = profileNotes,
                        onValueChange = { profileNotes = it },
                        label = { Text("Relationship Notes") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.insertSoftLoanProfile(name, null, phone.ifEmpty { null }, profileNotes)
                            showAddSoftProfileDialog = false
                        }
                    }
                ) {
                    Text("Create", color = specs.activeContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSoftProfileDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary)
                }
            }
        )
    }

    // --- ADD SOFT TRANSACTION DIALOG ---
    if (showAddSoftTxDialog) {
        var selectedProfile by remember { mutableStateOf<SoftLoanProfile?>(softProfiles.firstOrNull()) }
        var typeSelected by remember { mutableStateOf("BORROW") } // BORROW, GIVE, REPAY, RECEIVE
        var amountStr by remember { mutableStateOf("") }
        var accountSelected by remember { mutableStateOf(accounts.firstOrNull()) }
        var noteStr by remember { mutableStateOf("") }

        var profileMenuExpanded by remember { mutableStateOf(false) }
        var accountMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddSoftTxDialog = false },
            title = { Text("Record Soft Transaction", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Selection Dropdown
                    Text("RELATIONSHIP PROFILE", color = specs.contentSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(specs.inactiveContainer)
                                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                                .clickable { profileMenuExpanded = true }
                                .padding(14.dp)
                        ) {
                            Text(selectedProfile?.name ?: "No profiles created!", color = specs.contentPrimary, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = profileMenuExpanded, onDismissRequest = { profileMenuExpanded = false }) {
                            softProfiles.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        selectedProfile = p
                                        profileMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Transaction Type selector
                    Text("TRANSACTION TYPE", color = specs.contentSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("BORROW" to "Borrow", "GIVE" to "Give", "REPAY" to "Repay", "RECEIVE" to "Receive").forEach { (v, l) ->
                            val active = typeSelected == v
                            Button(
                                onClick = { typeSelected = v },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) specs.activeContainer else specs.inactiveContainer
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(l, color = if (active) specs.onActiveContainer else specs.contentPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    TextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount ($currency)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Account Selection Dropdown
                    Text("ASSOCIATED VAULT / ACCOUNT", color = specs.contentSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(specs.inactiveContainer)
                                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                                .clickable { accountMenuExpanded = true }
                                .padding(14.dp)
                        ) {
                            Text(accountSelected?.name ?: "No accounts created!", color = specs.contentPrimary, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = accountMenuExpanded, onDismissRequest = { accountMenuExpanded = false }) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.type}) • $currency${acc.balance.toInt()}") },
                                    onClick = {
                                        accountSelected = acc
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    TextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("Note / Remarks") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        val p = selectedProfile
                        val acc = accountSelected
                        if (p != null && acc != null && amount > 0.0) {
                            val actionNote = noteStr.ifBlank {
                                when (typeSelected) {
                                    "BORROW" -> "Borrowed from ${p.name}"
                                    "GIVE" -> "Gave to ${p.name}"
                                    "REPAY" -> "Repaid ${p.name}"
                                    "RECEIVE" -> "Received from ${p.name}"
                                    else -> "Activity with ${p.name}"
                                }
                            }
                            viewModel.addSoftLoanTransaction(
                                profileId = p.id,
                                type = typeSelected,
                                amount = amount,
                                accountId = acc.id,
                                note = actionNote
                            )
                            showAddSoftTxDialog = false
                        }
                    }
                ) {
                    Text("Save", color = specs.activeContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSoftTxDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary)
                }
            }
        )
    }

    // --- ADD FORMAL LENDER DIALOG ---
    if (showAddFormalLenderDialog) {
        var name by remember { mutableStateOf("") }
        var lenderNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFormalLenderDialog = false },
            title = { Text("Add Formal Lender", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Bank / Institution Name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = lenderNotes,
                        onValueChange = { lenderNotes = it },
                        label = { Text("Notes (Branches, Officers...)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.insertFormalLender(name, lenderNotes)
                            showAddFormalLenderDialog = false
                        }
                    }
                ) {
                    Text("Add", color = specs.activeContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFormalLenderDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary)
                }
            }
        )
    }

    // --- ADD FORMAL LOAN DIALOG ---
    if (showAddFormalLoanDialog) {
        var selectedLender by remember { mutableStateOf<FormalLender?>(formalLenders.firstOrNull()) }
        var loanAmtStr by remember { mutableStateOf("") }
        var interestStr by remember { mutableStateOf("") }
        var emiStr by remember { mutableStateOf("") }
        var direction by remember { mutableStateOf("BORROWED") } // BORROWED / LENT
        var startDateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var dueDateStr by remember { mutableStateOf("") }
        var statusSelection by remember { mutableStateOf("ACTIVE") }
        var loanNotes by remember { mutableStateOf("") }

        var lenderMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddFormalLoanDialog = false },
            title = { Text("Create Formal Facility", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("SELECT REGISTERED LENDER", color = specs.contentSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(specs.inactiveContainer)
                                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                                .clickable { lenderMenuExpanded = true }
                                .padding(14.dp)
                        ) {
                            Text(selectedLender?.name ?: "Please create a Lender profile first!", color = specs.contentPrimary, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = lenderMenuExpanded, onDismissRequest = { lenderMenuExpanded = false }) {
                            formalLenders.forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l.name) },
                                    onClick = {
                                        selectedLender = l
                                        lenderMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    TextField(
                        value = loanAmtStr,
                        onValueChange = { loanAmtStr = it },
                        label = { Text("Principal Amount ($currency)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = interestStr,
                        onValueChange = { interestStr = it },
                        label = { Text("Interest Rate % APR") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = emiStr,
                        onValueChange = { emiStr = it },
                        label = { Text("EMI / Installment Amount") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = startDateStr,
                        onValueChange = { startDateStr = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = dueDateStr,
                        onValueChange = { dueDateStr = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = loanNotes,
                        onValueChange = { loanNotes = it },
                        label = { Text("Agreement details / notes") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("FACILITY DIRECTION", color = specs.contentSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { direction = "BORROWED" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "BORROWED") specs.activeContainer else specs.inactiveContainer
                            )
                        ) {
                            Text("Borrowed", color = if (direction == "BORROWED") specs.onActiveContainer else specs.contentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { direction = "LENT" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "LENT") specs.activeContainer else specs.inactiveContainer
                            )
                        ) {
                            Text("Lent (Asset)", color = if (direction == "LENT") specs.onActiveContainer else specs.contentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = loanAmtStr.toDoubleOrNull() ?: 0.0
                        val lender = selectedLender
                        if (lender != null && amount > 0.0) {
                            val interest = interestStr.toDoubleOrNull() ?: 0.0
                            val emi = emiStr.toDoubleOrNull() ?: 0.0
                            val nextDue = try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                sdf.parse(dueDateStr.ifEmpty { startDateStr })?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            
                            viewModel.addLoan(
                                type = "FORMAL",
                                borrowerOrLender = lender.name,
                                amount = amount,
                                interest = interest,
                                emi = emi,
                                direction = direction,
                                nextDue = nextDue,
                                startDate = startDateStr,
                                dueDateString = dueDateStr,
                                notes = loanNotes,
                                status = statusSelection
                            )
                            showAddFormalLoanDialog = false
                        }
                    }
                ) {
                    Text("Save", color = specs.activeContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFormalLoanDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary)
                }
            }
        )
    }

    // --- RELATIONSHIP DETAIL PROFILE STATEMENT & RUNNING BALANCE LEDGER POPUP ---
    if (selectedSoftProfileForDetail.value != null) {
        val profile = selectedSoftProfileForDetail.value!!
        val profileTx = softLoanTransactions.filter { it.softLoanProfileId == profile.id }.sortedBy { it.timestamp }

        // Compute Running balance Bank Statement style
        var runningBal = 0.0
        val timelineLedger = profileTx.map { tx ->
            val change = when (tx.softLoanType) {
                "GIVE", "REPAY" -> tx.amount
                "BORROW", "RECEIVE" -> -tx.amount
                else -> 0.0
            }
            runningBal += change
            tx to runningBal
        }

        val totalBorrowed = profileTx.filter { it.softLoanType == "BORROW" }.sumOf { it.amount }
        val totalGiven = profileTx.filter { it.softLoanType == "GIVE" }.sumOf { it.amount }
        val totalRepaid = profileTx.filter { it.softLoanType == "REPAY" }.sumOf { it.amount }
        val totalReceived = profileTx.filter { it.softLoanType == "RECEIVE" }.sumOf { it.amount }

        val net = (totalGiven + totalRepaid) - (totalBorrowed + totalReceived)
        val payable = if (net < 0) -net else 0.0
        val receivable = if (net > 0) net else 0.0

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedSoftProfileForDetail.value = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .aureumGlass(shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("RELATIONSHIP LEDGER", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(profile.name, color = specs.contentPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { selectedSoftProfileForDetail.value = null }) {
                            Text("✕", color = specs.contentPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(specs.inactiveContainerBorder))

                    // Contact Details / notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phone: ${profile.phoneNumber ?: "N/A"}\nNotes: ${profile.notes.ifBlank { "None" }}",
                            color = specs.contentSecondary,
                            fontSize = 12.sp,
                            style = androidx.compose.ui.text.TextStyle(lineHeight = 16.sp)
                        )
                        
                        Button(
                            onClick = {
                                viewModel.deleteSoftLoanProfile(profile)
                                selectedSoftProfileForDetail.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("DELETE PROFILE", color = ExpenseCoral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Profile Statement Statistics Block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(specs.inactiveContainer)
                            .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("SUMMARY BALANCE STATISTICS", color = specs.contentSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Borrowed: $currency${totalBorrowed.toInt()}", color = specs.contentSecondary, fontSize = 11.sp)
                                Text("Total Lent: $currency${totalGiven.toInt()}", color = specs.contentSecondary, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Repaid: $currency${totalRepaid.toInt()}", color = specs.contentSecondary, fontSize = 11.sp)
                                Text("Total Received: $currency${totalReceived.toInt()}", color = specs.contentSecondary, fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(specs.inactiveContainerBorder))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CURRENT LEDGER BALANCE:", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = if (net > 0.0) "Receivable: $currency${receivable.toInt()}"
                                           else if (net < 0.0) "Payable: $currency${payable.toInt()}"
                                           else "Fully Settled (0.0)",
                                    color = if (net > 0.0) AssetGreen else if (net < 0.0) ExpenseCoral else specs.contentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Bank Statement running balance ledger timeline
                    Text("LEDGER STATEMENT RUNNING BALANCE", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    if (timelineLedger.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No transactions logged with this profile.", color = specs.contentSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(timelineLedger.reversed()) { (tx, bal) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (specs.isDark) Color.White.copy(alpha = 0.03f) else specs.inactiveContainer.copy(alpha = 0.5f))
                                        .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.06f) else specs.inactiveContainerBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (tx.softLoanType == "GIVE" || tx.softLoanType == "REPAY") AssetGreen.copy(alpha = 0.15f)
                                                            else ExpenseCoral.copy(alpha = 0.15f)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(tx.softLoanType ?: "ACTIVITY", color = if (tx.softLoanType == "GIVE" || tx.softLoanType == "REPAY") AssetGreen else ExpenseCoral, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text(
                                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tx.timestamp)),
                                                    color = specs.contentSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(tx.note, color = specs.contentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            val sign = if (tx.softLoanType == "GIVE" || tx.softLoanType == "REPAY") "+" else "-"
                                            Text(
                                                text = "$sign$currency${tx.amount.toInt()}",
                                                color = if (tx.softLoanType == "GIVE" || tx.softLoanType == "REPAY") AssetGreen else ExpenseCoral,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Bal: $currency${bal.toInt()}",
                                                color = if (bal >= 0.0) AssetGreen else ExpenseCoral,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- FORMAL LENDER DETAILS & HISTORY DIALOG ---
    if (selectedFormalLenderForDetail.value != null) {
        val lender = selectedFormalLenderForDetail.value!!
        val associatedLoans = loans.filter { it.type == "FORMAL" && it.borrowerOrLender == lender.name }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedFormalLenderForDetail.value = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(24.dp))
                    .aureumGlass(shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LENDER ACCOUNT PROFILE", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(lender.name, color = specs.contentPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { selectedFormalLenderForDetail.value = null }) {
                            Text("✕", color = specs.contentPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(specs.inactiveContainerBorder))

                    Text("Notes: ${lender.notes.ifBlank { "No description notes logged." }}", color = specs.contentSecondary, fontSize = 12.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active loans associated: ${associatedLoans.count { !it.isCompleted }}", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                viewModel.deleteFormalLender(lender)
                                selectedFormalLenderForDetail.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("DELETE PROFILE", color = ExpenseCoral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("ASSOCIATED CREDIT FACILITIES", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    if (associatedLoans.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No credit facilities mapped to this lender.", color = specs.contentSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(associatedLoans) { loan ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (specs.isDark) Color.White.copy(alpha = 0.03f) else specs.inactiveContainer)
                                        .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.06f) else specs.inactiveContainerBorder, RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = if (loan.direction == "BORROWED") "Borrowed (Liability)" else "Lent (Asset)",
                                                color = if (loan.direction == "BORROWED") ExpenseCoral else AssetGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text("APR: ${loan.interestRate}%", color = specs.contentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "Outstanding: $currency${loan.outstandingAmount.toInt()} / Principal: $currency${loan.loanAmount.toInt()}",
                                            color = specs.contentPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text("Date: ${loan.startDate} • Due: ${loan.dueDateString}", color = specs.contentSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormalRepayTimelineCard(
    loan: Loan,
    currency: String,
    onRepay: (Double) -> Unit
) {
    val progress = if (loan.loanAmount > 0.0) (1f - (loan.outstandingAmount / loan.loanAmount)).toFloat() else 1f
    val specs = AureumThemeStyles.specs
    var showRepayDialog by remember { mutableStateOf(false) }
    var repayAmountInput by remember { mutableStateOf("") }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(loan.borrowerOrLender, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Loan Type: ${if (loan.direction == "BORROWED") "Borrowing (Liability)" else "Lent (Asset)"}", 
                        color = specs.contentSecondary, 
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ExpenseCoral.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${loan.interestRate}% APR", color = ExpenseCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Timeline graphic node
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Start: ${loan.startDate.ifEmpty { "N/A" }}", color = specs.contentSecondary, fontSize = 10.sp)
                    Text("Due: ${loan.dueDateString.ifEmpty { "N/A" }}", color = specs.contentSecondary, fontSize = 10.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(specs.activeContainer)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(specs.inactiveContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(specs.activeContainer)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (loan.isCompleted) AssetGreen else ExpenseCoral)
                    )
                }
            }

            // High-fidelity payment notification panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (specs.isDark) Color.White.copy(alpha = 0.03f) else specs.inactiveContainer)
                    .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.06f) else specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val statusColor = when (loan.status) {
                                "OVERDUE" -> ExpenseCoral
                                "PAID" -> AssetGreen
                                else -> specs.activeContainer
                            }
                            Text("STATUS: ${loan.status}", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(
                                text = "Remaining: $currency${String.format(Locale.getDefault(), "%,.1f", loan.outstandingAmount)} / $currency${String.format(Locale.getDefault(), "%,.1f", loan.loanAmount)}",
                                color = specs.contentPrimary,
                                fontSize = 13.sp
                            )
                        }
                        
                        if (!loan.isCompleted) {
                            Button(
                                onClick = {
                                    repayAmountInput = loan.emiAmount.toString()
                                    showRepayDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("REPAY", color = specs.onActiveContainer, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }

                    if (loan.notes.isNotEmpty()) {
                        Text("Notes: ${loan.notes}", color = specs.contentSecondary, fontSize = 11.sp)
                    }

                    // Repayment histories / payment histories
                    if (loan.paymentsJson.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("PAYMENT HISTORY", color = specs.contentSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        loan.paymentsJson.split("|").forEach { payment ->
                            Text("• $payment", color = AssetGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    if (showRepayDialog) {
        AlertDialog(
            onDismissRequest = { showRepayDialog = false },
            title = { Text("Repay Loan", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter repayment amount to deduct from vaults.", color = specs.contentSecondary, fontSize = 13.sp)
                    TextField(
                        value = repayAmountInput,
                        onValueChange = { repayAmountInput = it },
                        label = { Text("Repayment Amount ($currency)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = repayAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0.0) {
                            onRepay(amount)
                            showRepayDialog = false
                        }
                    }
                ) {
                    Text("Confirm Pay", color = specs.activeContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepayDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// ADD TRANSACTION ENGINE
// -------------------------------------------------------------
@Composable
fun AddTransactionScreen(
    viewModel: AureumViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    var amountStr by remember { mutableStateOf("0") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, TRANSFER
    var selectedCategory by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(1) }
    var destinationAccountId by remember { mutableStateOf<Int?>(null) }
    var transferFeeStr by remember { mutableStateOf("0") }
    var receiptPath by remember { mutableStateOf<String?>(null) }

    val editingTx by viewModel.editingTransaction.collectAsState()

    androidx.compose.runtime.LaunchedEffect(editingTx) {
        editingTx?.let { tx ->
            amountStr = if (tx.amount % 1.0 == 0.0) tx.amount.toInt().toString() else tx.amount.toString()
            selectedType = tx.type
            selectedCategory = tx.category
            noteStr = tx.note
            selectedAccountId = tx.accountId
            destinationAccountId = tx.toAccountId
            transferFeeStr = if (tx.fee % 1.0 == 0.0) tx.fee.toInt().toString() else tx.fee.toString()
            receiptPath = tx.receiptPath
        }
    }

    var showKeyboard by remember { mutableStateOf(false) }
    var keypadTarget by remember { mutableStateOf("AMOUNT") } // AMOUNT, FEE

    val allCategories by viewModel.allCategories.collectAsState()
    val filteredCategories = allCategories.filter { it.type == selectedType }

    androidx.compose.runtime.LaunchedEffect(selectedType, filteredCategories) {
        if (filteredCategories.isNotEmpty()) {
            if (filteredCategories.none { it.name == selectedCategory }) {
                selectedCategory = filteredCategories.first().name
            }
        }
    }

    val categories = listOf("Dining", "Retail", "Housing", "Salary", "Transport", "Internal")
    val specs = AureumThemeStyles.specs
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = copyUriToInternalStorage(context, it)
            if (path != null) {
                receiptPath = path
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 120.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TRANSACTION CREATION",
                color = specs.contentSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = specs.inactiveContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
            ) {
                Text("Dismiss", color = specs.contentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Mode switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("EXPENSE", "INCOME", "TRANSFER").forEach { t ->
                val active = selectedType == t
                Button(
                    onClick = { 
                        selectedType = t 
                        if (t != "TRANSFER") destinationAccountId = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (!active) Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp)) else Modifier
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) specs.activeContainer else specs.inactiveContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t, fontSize = 10.sp, color = if (active) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Amount Display (Tapping launches the keypad dialog)
        Text("TRANSACTION AMOUNT", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(specs.inactiveContainer)
                .border(1.dp, specs.activeContainer.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .clickable {
                    keypadTarget = "AMOUNT"
                    showKeyboard = true
                }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currency$amountStr",
                    color = if (selectedType == "INCOME") AssetGreen else if (selectedType == "EXPENSE") ExpenseCoral else specs.contentPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to edit amount",
                    color = specs.contentSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Category selection grid (Regular expenses/incomes only)
        if (selectedType != "TRANSFER") {
            Text("SELECT CATEGORY", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredCategories.forEach { cat ->
                    val active = selectedCategory == cat.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) specs.activeContainer else specs.inactiveContainer)
                            .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = cat.name }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AureumVectorIcon(
                                name = cat.icon,
                                tint = if (active) specs.onActiveContainer else specs.contentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(cat.name, color = if (active) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Source Account
        Text("SRC ACCOUNT", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { acc ->
                val active = selectedAccountId == acc.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) specs.activeContainer else specs.inactiveContainer)
                        .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                        .clickable { 
                            selectedAccountId = acc.id 
                            if (destinationAccountId == acc.id) {
                                destinationAccountId = null
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(acc.name, color = if (active) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Destination Account & Fee for Transfers
        if (selectedType == "TRANSFER") {
            Text("DEST ACCOUNT", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                    val active = destinationAccountId == acc.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) specs.activeContainer else specs.inactiveContainer)
                            .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                            .clickable { destinationAccountId = acc.id }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(acc.name, color = if (active) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("TRANSFER FEE (OPTIONAL)", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(specs.inactiveContainer)
                    .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        keypadTarget = "FEE"
                        showKeyboard = true
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currency$transferFeeStr",
                        color = specs.contentPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to enter transfer fee",
                        color = specs.contentSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Memo field descriptions
        Text("MEMO / DESCRIPTION", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        TextField(
            value = noteStr,
            onValueChange = { noteStr = it },
            placeholder = { Text("Add transaction description...", color = specs.contentSecondary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = specs.inactiveContainer,
                unfocusedContainerColor = specs.inactiveContainer,
                focusedTextColor = specs.contentPrimary,
                unfocusedTextColor = specs.contentPrimary,
                focusedIndicatorColor = specs.activeContainer,
                unfocusedIndicatorColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
        )

        // Receipt / Image picking section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ATTACH RECEIPT IMAGE (OPTIONAL)", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (receiptPath == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(specs.inactiveContainer)
                        .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AureumVectorIcon(name = "image", tint = specs.contentSecondary, modifier = Modifier.size(24.dp))
                        Text("Add Receipt / Image from Device", color = specs.contentSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(specs.inactiveContainer)
                        .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = receiptPath,
                        contentDescription = "Picked Receipt Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    IconButton(
                        onClick = { receiptPath = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                val value = amountStr.toDoubleOrNull() ?: 0.0
                val feeVal = if (selectedType == "TRANSFER") (transferFeeStr.toDoubleOrNull() ?: 0.0) else 0.0
                if (value > 0.0) {
                    val currentEdit = viewModel.editingTransaction.value
                    if (currentEdit != null) {
                        viewModel.editTransaction(
                            oldTransaction = currentEdit,
                            type = selectedType,
                            amount = value,
                            category = if (selectedType == "TRANSFER") "Internal" else selectedCategory,
                            accountId = selectedAccountId,
                            toAccountId = destinationAccountId,
                            note = noteStr,
                            timestamp = currentEdit.timestamp,
                            receiptPath = receiptPath,
                            fee = feeVal
                        )
                        viewModel.editingTransaction.value = null
                    } else {
                        viewModel.addTransaction(
                            type = selectedType,
                            amount = value,
                            category = if (selectedType == "TRANSFER") "Internal" else selectedCategory,
                            accountId = selectedAccountId,
                            toAccountId = destinationAccountId,
                            note = noteStr,
                            timestamp = System.currentTimeMillis(),
                            receiptPath = receiptPath,
                            fee = feeVal
                        )
                    }
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (editingTx != null) "UPDATE TRANSACTION" else "LOG TRANSACTION", color = specs.onActiveContainer, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }

    // Number keyboard overlay popup dialog
    if (showKeyboard) {
        val targetLabel = if (keypadTarget == "AMOUNT") "Transaction Amount" else "Transfer Fee (Optional)"
        val currentVal = if (keypadTarget == "AMOUNT") amountStr else transferFeeStr
        
        AlertDialog(
            onDismissRequest = { showKeyboard = false },
            title = {
                Text(
                    text = "Enter $targetLabel",
                    color = specs.contentPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(specs.inactiveContainer)
                            .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$currency$currentVal",
                            color = if (keypadTarget == "AMOUNT") {
                                if (selectedType == "INCOME") AssetGreen else if (selectedType == "EXPENSE") ExpenseCoral else specs.contentPrimary
                            } else {
                                specs.contentSecondary
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    NumericKeypad(
                        onKeyPress = { key ->
                            if (keypadTarget == "AMOUNT") {
                                if (key == "⌫") {
                                    amountStr = if (amountStr.length <= 1) "0" else amountStr.dropLast(1)
                                } else if (key == ".") {
                                    if (!amountStr.contains(".")) amountStr += "."
                                } else {
                                    if (amountStr == "0") amountStr = key else amountStr += key
                                }
                            } else {
                                if (key == "⌫") {
                                    transferFeeStr = if (transferFeeStr.length <= 1) "0" else transferFeeStr.dropLast(1)
                                } else if (key == ".") {
                                    if (!transferFeeStr.contains(".")) transferFeeStr += "."
                                } else {
                                    if (transferFeeStr == "0") transferFeeStr = key else transferFeeStr += key
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showKeyboard = false },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun NumericKeypad(onKeyPress: (String) -> Unit) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )
    val specs = AureumThemeStyles.specs

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(specs.inactiveContainer)
                            .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                            .clickable { onKeyPress(char) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(char, color = specs.contentPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SETTINGS / MORE SCREEN
// -------------------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: AureumViewModel,
    modifier: Modifier = Modifier
) {
    val currency by viewModel.selectedCurrency.collectAsState()
    val themeOverride by viewModel.isDarkTheme.collectAsState()
    val exportStatusMsg by viewModel.exportMessage.collectAsState()
    val secureMode by viewModel.secureModeEnabled.collectAsState()

    val isPinGuardEnabled by viewModel.isPinGuardEnabled.collectAsState()
    val correctPin by viewModel.correctPin.collectAsState()
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    var showWipeDataConfirm by remember { mutableStateOf(false) }
    var wipeDataPinInput by remember { mutableStateOf("") }
    var wipeDataError by remember { mutableStateOf(false) }

    var showCategoryManager by remember { mutableStateOf(false) }
    var showRestoreDefaultsConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val specs = AureumThemeStyles.specs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "PREFERENCES & MANAGEMENT",
            color = specs.contentSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        // Currency Setting
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("PREFERRED CURRENCY SYMBOL", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("৳", "$", "€", "£").forEach { c ->
                    val active = currency == c
                    Button(
                        onClick = { viewModel.selectedCurrency.value = c },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) specs.activeContainer else specs.inactiveContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (!active) Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(10.dp)) else Modifier
                            ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(c, color = if (active) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Security Toggles
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("LEDGER ACCESS SECURITY", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Lock Guard", color = specs.contentPrimary, fontSize = 14.sp)
                    Text("Protect visual states on hide", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = isPinGuardEnabled,
                    onCheckedChange = { viewModel.setPinGuardEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = specs.activeContainer)
                )
            }

            if (isPinGuardEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(specs.inactiveContainerBorder))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lock Screen Mode", color = specs.contentPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Active PIN passcode: $correctPin", color = specs.contentSecondary, fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { 
                                newPinInput = ""
                                showChangePinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = specs.inactiveContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                        ) {
                            Text("Change PIN", color = specs.contentPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.activateLock() },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, ExpenseCoral.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        ) {
                            Text("Lock Now", color = ExpenseCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(specs.inactiveContainerBorder))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Secure Anti-Screenshot Mode", color = specs.contentPrimary, fontSize = 14.sp)
                    Text("Enable Window Secure flag", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = secureMode,
                    onCheckedChange = { viewModel.secureModeEnabled.value = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = specs.activeContainer)
                )
            }
        }

        // Offline File Exporter CSV Box
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CSV OFFLINE LEDGER EXPORT", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Generates clean CSV of all savings, cash sheets", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { viewModel.exportLedgerToCsv() },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Export", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Tag Custom Category management Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MANAGE TRANSACTION CATEGORIES", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Customize income/expense category names, icons, and items", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { showCategoryManager = true },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Manage", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Setup restore defaults options
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("RESTORE DEFAULT SETTINGS", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Resets all categories, templates, and layouts to default values safely", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { showRestoreDefaultsConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.inactiveContainer),
                    modifier = Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                ) {
                    Text("Restore", color = specs.contentPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Secure Delete All Data option directly after CSV Export
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DELETE ALL APPLICATION DATA", color = ExpenseCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Wipes all local ledger files, custom vaults, and transactions", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { 
                        wipeDataPinInput = ""
                        wipeDataError = false
                        showWipeDataConfirm = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Export output bottom sheet alert
    if (exportStatusMsg != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportMessage() },
            title = { Text("CSV EXPORT COMPLETED", color = Color.White) },
            text = {
                Text(
                    text = exportStatusMsg ?: "",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportStatusMsg ?: ""))
                        viewModel.clearExportMessage()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Copy to Clipboard", color = specs.onActiveContainer)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportMessage() }) {
                    Text("Dismiss", color = Color.White)
                }
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Customize Passcode PIN", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set a custom 4-digit security code PIN to restrict app access.",
                        color = specs.contentSecondary,
                        fontSize = 13.sp
                    )
                    TextField(
                        value = newPinInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                newPinInput = input
                            }
                        },
                        label = { Text("New 4-Digit PIN") },
                        placeholder = { Text("e.g. 2580") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer,
                            focusedIndicatorColor = specs.activeContainer,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length == 4) {
                            viewModel.saveNewPin(newPinInput)
                            android.widget.Toast.makeText(context, "Passcode PIN saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            showChangePinDialog = false
                        } else {
                            android.widget.Toast.makeText(context, "PIN must be exactly 4 digits!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Save PIN", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showWipeDataConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeDataConfirm = false },
            title = { Text("Secured Database Wipe", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "To authorize deleting all accounts, transactions, and loan data, please enter the App Security PIN (Default PIN: 1234):",
                        color = specs.contentSecondary,
                        fontSize = 13.sp
                    )
                    TextField(
                        value = wipeDataPinInput,
                        onValueChange = { 
                            wipeDataPinInput = it
                            wipeDataError = false
                        },
                        label = { Text("Enter App PIN") },
                        isError = wipeDataError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (wipeDataError) {
                        Text(
                            text = "Incorrect security PIN. Please try again.",
                            color = ExpenseCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (wipeDataPinInput == correctPin) {
                            viewModel.deleteAllData()
                            showWipeDataConfirm = false
                            wipeDataPinInput = ""
                            android.widget.Toast.makeText(context, "All app ledgers and database wiped successfully!", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            wipeDataError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral)
                ) {
                    Text("Deauthorize & Wipe", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDataConfirm = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRestoreDefaultsConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreDefaultsConfirm = false },
            title = { Text("Restore Settings to Default?", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will restore default category templates, default dashboard configurations and settings.\n\nTransactions, active vaults, and loan history always remain safe and intact.",
                    color = specs.contentSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDefaultSettings()
                        showRestoreDefaultsConfirm = false
                        android.widget.Toast.makeText(context, "Default settings and category templates restored successfully!", android.widget.Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Restore Defaults", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDefaultsConfirm = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCategoryManager) {
        CategoryManagerOverlay(
            viewModel = viewModel,
            onDismiss = { showCategoryManager = false }
        )
    }
}

@Composable
fun CalculatorCompactWidget(viewModel: AureumViewModel) {
    val calcExpr by viewModel.calcExpression.collectAsState()
    val calcRes by viewModel.calcResult.collectAsState()
    val specs = AureumThemeStyles.specs

    val calcKeys = listOf(
        listOf("7", "8", "9", "+"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "×"),
        listOf("C", "0", "=", "÷")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(specs.inactiveContainer)
                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text(calcExpr.ifBlank { "0" }, color = specs.contentPrimary, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(calcRes, color = specs.headingColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        calcKeys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { char ->
                    val color = when (char) {
                        "C", "=" -> specs.activeContainer.copy(alpha = if (specs.isDark) 0.15f else 0.35f)
                        "+", "-", "×", "÷" -> specs.headingColor.copy(alpha = if (specs.isDark) 0.15f else 0.25f)
                        else -> specs.inactiveContainer
                    }
                    val textColor = when (char) {
                        "C", "=" -> if (specs.isDark) specs.activeContainer else Color(0xFF2E7D32)
                        "+", "-", "×", "÷" -> specs.headingColor
                        else -> specs.contentPrimary
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .then(
                                if (char != "C" && char != "=" && char != "+" && char != "-" && char != "×" && char != "÷") {
                                    Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                                } else Modifier
                            )
                            .clickable { viewModel.onCalcPress(char) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(char, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryManagerOverlay(
    viewModel: AureumViewModel,
    onDismiss: () -> Unit
) {
    val allCategories by viewModel.allCategories.collectAsState()
    var selectedTypeTab by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME"
    val specs = AureumThemeStyles.specs

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var addCategoryName by remember { mutableStateOf("") }
    var addCategoryIcon by remember { mutableStateOf("circle") }

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editCategoryName by remember { mutableStateOf("") }
    var editCategoryIcon by remember { mutableStateOf("") }

    val filteredList = allCategories.filter { it.type == selectedTypeTab }

    // List of Icon options
    val expenseIcons = listOf("utensils", "shirt", "home", "building", "wifi", "bus", "heart_pulse", "sparkles", "graduation_cap", "gift", "gamepad", "circle")
    val incomeIcons = listOf("wallet", "hand_coins", "trending_up", "gift", "circle")

    val iconsToChoose = if (selectedTypeTab == "EXPENSE") expenseIcons else incomeIcons
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category Settings", color = specs.contentPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = specs.contentPrimary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EXPENSE", "INCOME").forEach { type ->
                        val active = selectedTypeTab == type
                        Button(
                            onClick = { selectedTypeTab = type },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) specs.activeContainer else specs.inactiveContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(type, color = if (active) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable List of Categories
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(specs.inactiveContainer)
                                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Category Icon on Left side
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(specs.activeContainer.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AureumVectorIcon(name = cat.icon, tint = specs.contentPrimary, modifier = Modifier.size(18.dp))
                                }

                                Column {
                                    Text(cat.name, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (cat.isDefault) {
                                        Text("System Category", color = specs.contentSecondary, fontSize = 10.sp)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Edit Button (Pencil Icon)
                                IconButton(
                                    onClick = {
                                        editingCategory = cat
                                        editCategoryName = cat.name
                                        editCategoryIcon = cat.icon
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = specs.contentSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Button (Trash Icon) - Deletable if not "Other" or system categories
                                if (cat.isEditable && cat.name.lowercase() != "other") {
                                    IconButton(
                                        onClick = { viewModel.deleteCategory(cat) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ExpenseCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Create category CTA button
                Button(
                    onClick = {
                        addCategoryName = ""
                        addCategoryIcon = iconsToChoose.first()
                        showAddDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Create Custom Category", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = specs.contentPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )

    // --- ADD CATEGORY DIALOG ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create ${selectedTypeTab.lowercase()} Category", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = addCategoryName,
                        onValueChange = { addCategoryName = it },
                        label = { Text("Category Name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("SELECT ICON", color = specs.contentSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)

                    // Icons Picker Grid
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(130.dp)
                    ) {
                        items(count = iconsToChoose.size) { index ->
                            val iconName = iconsToChoose[index]
                            val selected = addCategoryIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) specs.activeContainer else specs.inactiveContainer)
                                    .border(2.dp, if (selected) specs.contentPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { addCategoryIcon = iconName }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AureumVectorIcon(name = iconName, tint = if (selected) specs.onActiveContainer else specs.contentPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addCategoryName.isNotBlank()) {
                            viewModel.addCategory(addCategoryName, addCategoryIcon, selectedTypeTab)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Create", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- EDIT CATEGORY DIALOG ---
    val currentEditing = editingCategory
    if (currentEditing != null) {
        val targetCat = currentEditing
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Edit Category", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it },
                        label = { Text("Category Name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("SELECT ICON", color = specs.contentSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)

                    // Icons Picker Grid
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(130.dp)
                    ) {
                        items(count = iconsToChoose.size) { index ->
                            val iconName = iconsToChoose[index]
                            val selected = editCategoryIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) specs.activeContainer else specs.inactiveContainer)
                                    .border(2.dp, if (selected) specs.contentPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { editCategoryIcon = iconName }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AureumVectorIcon(name = iconName, tint = if (selected) specs.onActiveContainer else specs.contentPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editCategoryName.isNotBlank()) {
                            viewModel.updateCategory(targetCat.copy(name = editCategoryName, icon = editCategoryIcon))
                            editingCategory = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Save", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
