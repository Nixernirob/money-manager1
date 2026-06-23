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
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AureumViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.luminance

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
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    // Calculate total net worth
    val totalNetWorth = accounts.sumOf { 
        if (it.type == "CREDIT") -it.balance else it.balance 
    }

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
                            currency = currency
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

                    val alerts = if (todaySpent > 20000) "Limit reached" else "Secure safe"

                    InsightChip(
                        label = "Spent Today",
                        value = "$currency${todaySpent.toInt()}",
                        accentColor = ExpenseCoral,
                        modifier = Modifier.weight(1f)
                    )
                    InsightChip(
                        label = "Alert Status",
                        value = alerts,
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
                    onDelete = { viewModel.deleteTransaction(trans) }
                )
            }
        }
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
    currency: String
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
                        "BANK" -> "bank"
                        "WALLET" -> "vault"
                        "CREDIT" -> "credit_card"
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
    onDelete: () -> Unit
) {
    var showMoreActions by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val rowBackground = if (isDark) Color.White.copy(alpha = 0.03f) else Color.White
    val rowBorderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE0E2EC)
    val noteColor = if (isDark) Color.White else Color(0xFF1A1C1E)
    val categoryColor = if (isDark) TextSecondary else Color(0xFF74777F)
    val amountColor = if (transaction.type == "INCOME") {
        if (isDark) AssetGreen else Color(0xFF2E7D32)
    } else {
        if (isDark) Color.White else Color(0xFF1A1C1E)
    }
    
    val avatarBg = if (transaction.type == "INCOME") {
        if (isDark) AssetGreen.copy(alpha = 0.15f) else Color(0xFFE8F3D6)
    } else {
        if (isDark) ExpenseCoral.copy(alpha = 0.15f) else Color(0xFFF0F0F7)
    }
    
    val avatarIconTint = if (transaction.type == "INCOME") {
        if (isDark) AssetGreen else Color(0xFF2D3325)
    } else {
        if (isDark) ExpenseCoral else Color(0xFF44474E)
    }

    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE0E2EC)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBackground)
            .border(1.dp, rowBorderColor, RoundedCornerShape(16.dp))
            .clickable { showMoreActions = !showMoreActions }
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
                        name = when (transaction.category.lowercase()) {
                            "housing" -> "bank"
                            "retail", "shopping" -> "retail"
                            "dining", "food" -> "dining"
                            "salary" -> "income"
                            "internal", "loans" -> "transfer"
                            else -> "expense"
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
                    Text(
                        text = transaction.category,
                        color = categoryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type == "INCOME") "+" else "-"}$currency${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
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

        AnimatedVisibility(
            visible = showMoreActions,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = dividerColor)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (transaction.receiptPath != null) {
                        Text(
                            text = "📎 Attached Receipt",
                            color = if (isDark) MidnightPrimary else Color(0xFF005AC1),
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { /* Simulate display */ }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Delete", color = ExpenseCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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

        // Heatmap Calendar visualization
        CalendarHeatmap(transactions = transactions)

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
                        onDelete = { viewModel.deleteTransaction(trans) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeatmap(transactions: List<Transaction>) {
    // Renders a high-fidelity color heatmap grid representing transactions logged daily over past 4 weeks
    val daysCount = 28
    val itemsMap = remember(transactions) {
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        val statsMap = mutableMapOf<String, Int>()
        transactions.forEach {
            val key = formatter.format(Date(it.timestamp))
            statsMap[key] = (statsMap[key] ?: 0) + 1
        }
        statsMap
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
            text = "ACTIVITY HEATMAP",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -daysCount + 1)
            val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)

            for (w in 0 until 4) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (d in 0 until 7) {
                        val key = formatter.format(calendar.time)
                        val count = itemsMap[key] ?: 0
                        val color = when {
                            count == 0 -> if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                            count == 1 -> MidnightPrimary.copy(alpha = 0.45f)
                            count == 2 -> MidnightPrimary.copy(alpha = 0.75f)
                            else -> MidnightPrimary // Intense glow
                        }
                        calendar.add(Calendar.DAY_OF_YEAR, 1)

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("• High Activity", color = MidnightPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("• Low Activity", color = MidnightPrimary.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    var selectedType by remember { mutableStateOf("BANK") } // BANK, CASH, CREDIT

    val assetsTotal = accounts.filter { it.type != "CREDIT" }.sumOf { it.balance }
    val creditOutflow = accounts.filter { it.type == "CREDIT" }.sumOf { it.balance }
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
                onClick = { showAddAccountDialog = true },
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
                            color = if (acc.type == "CREDIT") ExpenseCoral else specs.contentPrimary,
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
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BANK", "WALLET", "CREDIT").forEach { t ->
                            Button(
                                onClick = { selectedType = t },
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (selectedType != t) Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(100)) else Modifier
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == t) specs.activeContainer else specs.inactiveContainer
                                )
                            ) {
                                Text(t, fontSize = 10.sp, color = if (selectedType == t) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selectedType == "CREDIT") {
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
}

// -------------------------------------------------------------
// LOANS REPAYMENT TIMELINE & SOFT LOANS SYSTEM
// -------------------------------------------------------------
@Composable
fun LoansScreen(
    viewModel: AureumViewModel,
    modifier: Modifier = Modifier
) {
    val loans by viewModel.allLoans.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()

    var showFormalLoansMode by remember { mutableStateOf(true) }
    val specs = AureumThemeStyles.specs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "LOANS & CREDIT CHANNELS",
            color = specs.contentSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Switch Tab
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
                Text("FORMAL FACILITIES", color = if (showFormalLoansMode) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold)
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
                Text("SOFT CHANNELS", color = if (!showFormalLoansMode) specs.onActiveContainer else specs.contentPrimary, fontWeight = FontWeight.Bold)
            }
        }

        // List representation
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.weight(1f)
        ) {
            val categorisedLoans = loans.filter { 
                if (showFormalLoansMode) it.type == "FORMAL" else it.type == "SOFT"
            }

            if (categorisedLoans.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No loans active in this segment.", color = specs.contentSecondary)
                    }
                }
            } else {
                items(categorisedLoans) { loan ->
                    if (showFormalLoansMode) {
                        FormalRepayTimelineCard(loan = loan, currency = currency) {
                            viewModel.repayLoanAmount(loan, loan.emiAmount)
                        }
                    } else {
                        SoftLoanFlowCard(loan = loan, currency = currency) {
                            viewModel.markSoftLoanReturned(loan)
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
    onEmiRepay: () -> Unit
) {
    val daysOverdue = 3 // Dec 15 vs simulated system time
    val progress = (1f - (loan.outstandingAmount / loan.loanAmount)).toFloat()
    val specs = AureumThemeStyles.specs

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(loan.borrowerOrLender, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Next: Dec 15 (Scheduled)", color = specs.contentSecondary, fontSize = 11.sp)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(specs.inactiveContainer)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
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
                        .background(ExpenseCoral)
                )
            }

            // High-fidelity Overdue notification panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ExpenseCoral.copy(alpha = 0.1f))
                    .border(1.dp, ExpenseCoral.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("⚠ OVERDUE $daysOverdue DAYS", color = ExpenseCoral, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("$currency${String.format(Locale.getDefault(), "%,.0f", loan.emiAmount)} outstanding EMI", color = specs.contentPrimary, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onEmiRepay,
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("PAY NOW", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SoftLoanFlowCard(
    loan: Loan,
    currency: String,
    onMarkReturned: () -> Unit
) {
    val isGiving = loan.direction == "LENT"
    val specs = AureumThemeStyles.specs

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(specs.inactiveContainer)
            .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isGiving) AssetGreen.copy(alpha = 0.15f) else ExpenseCoral.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // User avatar name representation
                        Text(
                            loan.borrowerOrLender.take(2).uppercase(),
                            color = if (isGiving) AssetGreen else ExpenseCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Column {
                        Text(loan.borrowerOrLender, color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (isGiving) "YOU LENT" else "YOU BORROWED", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "$currency${loan.outstandingAmount.toInt()}",
                        color = if (isGiving) AssetGreen else ExpenseCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    AureumVectorIcon(
                        name = "transfer",
                        tint = if (isGiving) AssetGreen else ExpenseCoral,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Arrow visualization diagram
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGiving) " Borrower's Account ←─── $currency${loan.outstandingAmount.toInt()} ─── You"
                           else " Lender's Account ─── $currency${loan.outstandingAmount.toInt()} ───→ You",
                    color = specs.contentSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (!loan.isCompleted) {
                Button(
                    onClick = onMarkReturned,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightPrimary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mark Returned", color = MidnightPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
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
    var selectedCategory by remember { mutableStateOf("Dining") }
    var noteStr by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(1) }
    var destinationAccountId by remember { mutableStateOf<Int?>(null) }

    val categories = listOf("Dining", "Retail", "Housing", "Salary", "Transport", "Internal")

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
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Text("Dismiss", color = Color.White, fontSize = 11.sp)
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) MidnightTertiary else Color.White.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t, fontSize = 10.sp, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Amount Display (High frequency vector counter)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$currency$amountStr",
                color = if (selectedType == "INCOME") AssetGreen else Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Category selection grid
        Text("SELECT CATEGORY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) MidnightPrimary else Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(cat, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Account selectors
        Text("SRC ACCOUNT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        .background(if (active) MidnightPrimary else Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .clickable { selectedAccountId = acc.id }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(acc.name, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (selectedType == "TRANSFER") {
            Text("DEST ACCOUNT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            .background(if (active) MidnightPrimary else Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                            .clickable { destinationAccountId = acc.id }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(acc.name, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Memo field inputs
        TextField(
            value = noteStr,
            onValueChange = { noteStr = it },
            placeholder = { Text("Add transaction description...", color = TextSecondary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = MidnightTertiary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Built-in Numeric Keypad
        NumericKeypad(
            onKeyPress = { key ->
                if (key == "⌫") {
                    amountStr = if (amountStr.length <= 1) "0" else amountStr.dropLast(1)
                } else if (key == ".") {
                    if (!amountStr.contains(".")) amountStr += "."
                } else {
                    if (amountStr == "0") amountStr = key else amountStr += key
                }
            }
        )

        // Save Button
        Button(
            onClick = {
                val value = amountStr.toDoubleOrNull() ?: 0.0
                if (value > 0.0) {
                    viewModel.addTransaction(
                        type = selectedType,
                        amount = value,
                        category = selectedCategory,
                        accountId = selectedAccountId,
                        toAccountId = destinationAccountId,
                        note = noteStr,
                        timestamp = System.currentTimeMillis()
                    )
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightTertiary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("LOG TRANSACTION", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
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
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onKeyPress(char) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(char, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val specs = AureumThemeStyles.specs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 120.dp)
            .verticalScroll(rememberScrollState()),
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

        // Custom Calculator Utility Box
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("BUDGET CALCULATOR TOOL", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            CalculatorCompactWidget(viewModel = viewModel)
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
                    checked = viewModel.isLocked.value,
                    onCheckedChange = { viewModel.isLocked.value = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = specs.activeContainer)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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

        // Soft Reset database options simulation
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Reset Application to Defaults",
            color = ExpenseCoral,
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable {
                    // Simulate wiping db / relaunch callback
                }
        )
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
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightTertiary)
                ) {
                    Text("Copy to Clipboard", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportMessage() }) {
                    Text("Dismiss", color = Color.White)
                }
            }
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
