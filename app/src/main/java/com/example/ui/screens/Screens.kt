package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
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
import kotlinx.coroutines.launch
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

val vaultPresetColors = listOf(
    "#10B981" to "Emerald",
    "#3B82F6" to "Royal Blue",
    "#F59E0B" to "Amber",
    "#EF4444" to "Crimson",
    "#8B5CF6" to "Purple",
    "#06B6D4" to "Cyan",
    "#EC4899" to "Pink",
    "#1E2430" to "Obsidian",
    "#6366F1" to "Indigo",
    "#B0FF27" to "Neon Lime"
)

fun getVaultGradientColor(tintHex: String): List<Color> {
    return try {
        val base = Color(android.graphics.Color.parseColor(tintHex))
        val dark = Color(
            red = (base.red * 0.45f).coerceIn(0f, 1f),
            green = (base.green * 0.45f).coerceIn(0f, 1f),
            blue = (base.blue * 0.45f).coerceIn(0f, 1f),
            alpha = 1f
        )
        listOf(base, dark)
    } catch (e: Exception) {
        listOf(Color(0xFF1E2430), Color(0xFF11141A))
    }
}

private data class VaultCardModel(
    val id: Int,
    val name: String,
    val balance: Double,
    val dueAmount: Double,
    val tag: String,
    val iconName: String,
    val gradient: List<Color>,
    val textColor: Color,
    val subTextColor: Color,
    val buttonBg: Color,
    val buttonTextColor: Color,
    val buttonLabel: String,
    val account: Account?
)

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
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var showEditVaultDialog by remember { mutableStateOf(false) }
    var editingVaultAccount by remember { mutableStateOf<Account?>(null) }

    var vaultNameInput by remember { mutableStateOf("") }
    var vaultBalanceInput by remember { mutableStateOf("") }
    var vaultCreditLimitInput by remember { mutableStateOf("") }
    var vaultTypeInput by remember { mutableStateOf("CASH") }
    var vaultColorInput by remember { mutableStateOf("#10B981") }

    var editVaultName by remember { mutableStateOf("") }
    var editVaultBalance by remember { mutableStateOf("") }
    var editVaultCreditLimit by remember { mutableStateOf("") }
    var editVaultType by remember { mutableStateOf("CASH") }
    var editVaultColor by remember { mutableStateOf("#10B981") }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

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
        if (it.type == "CREDIT" || it.type == "CARD") -it.balance else it.balance 
    }
    val formalLentLoans = loans.filter { it.type == "FORMAL" && it.direction == "LENT" }.sumOf { it.outstandingAmount }
    val formalBorrowedLoans = loans.filter { it.type == "FORMAL" && it.direction == "BORROWED" }.sumOf { it.outstandingAmount }
    val totalNetWorth = accountsNetWorth + totalSoftReceivable - totalSoftPayable + formalLentLoans - formalBorrowedLoans

    val specs = AureumThemeStyles.specs
    val primaryCardBg = if (isDark) SurfaceContainer else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEFF1F5)

    // Swipeable Vault Accounts Card Models (Right & Left Swipeable)
    val vaultCardList = remember(accounts, totalNetWorth, totalSoftPayable, formalBorrowedLoans) {
        if (accounts.isEmpty()) {
            listOf(
                VaultCardModel(
                    id = -1,
                    name = "Cash Vault",
                    balance = 0.0,
                    dueAmount = 0.0,
                    tag = "CASH VAULT",
                    iconName = "savings",
                    gradient = getVaultGradientColor("#10B981"),
                    textColor = Color.White,
                    subTextColor = Color.White.copy(alpha = 0.8f),
                    buttonBg = Color.White.copy(alpha = 0.25f),
                    buttonTextColor = Color.White,
                    buttonLabel = "Add",
                    account = null
                )
            )
        } else {
            // First card: All Wealth / Total Net Worth Hero Card
            val list = mutableListOf(
                VaultCardModel(
                    id = 0,
                    name = "Total Net Worth",
                    balance = totalNetWorth,
                    dueAmount = if (totalSoftPayable > 0) totalSoftPayable else (formalBorrowedLoans.takeIf { it > 0 } ?: 0.0),
                    tag = "ALL WEALTH",
                    iconName = "account_balance",
                    gradient = listOf(NeonLimeLight, NeonLimeDark),
                    textColor = Color.Black,
                    subTextColor = Color(0xFF1C3A0A),
                    buttonBg = CardBlack,
                    buttonTextColor = Color.White,
                    buttonLabel = "Add Tx",
                    account = null
                )
            )

            // Add each individual vault account as its own swipeable card with its custom color gradient
            accounts.forEach { acc ->
                val grad = getVaultGradientColor(acc.tintHex)
                val isLime = acc.tintHex.equals("#B0FF27", ignoreCase = true)
                val tagStr = when (acc.type.uppercase()) {
                    "ACCOUNTS", "BANK" -> "BANK VAULT"
                    "CASH", "WALLET" -> "CASH VAULT"
                    "CARD", "CREDIT" -> "CREDIT VAULT"
                    else -> "ACTIVE VAULT"
                }
                list.add(
                    VaultCardModel(
                        id = acc.id,
                        name = acc.name,
                        balance = acc.balance,
                        dueAmount = if (acc.type.uppercase() in listOf("CARD", "CREDIT")) acc.balance else 0.0,
                        tag = tagStr,
                        iconName = acc.iconName,
                        gradient = grad,
                        textColor = if (isLime) Color.Black else Color.White,
                        subTextColor = if (isLime) Color(0xFF1C3A0A) else Color.White.copy(alpha = 0.8f),
                        buttonBg = if (isLime) CardBlack else Color.White.copy(alpha = 0.25f),
                        buttonTextColor = Color.White,
                        buttonLabel = "Edit",
                        account = acc
                    )
                )
            }
            list
        }
    }

    val cardPagerState = rememberPagerState(pageCount = { vaultCardList.size })

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        // 1. Clean Top Header Bar (Removed notification bar and profile icon as requested)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallet",
                    color = specs.contentPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                // Eye Toggle for Balance Privacy
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(primaryCardBg)
                        .border(1.dp, cardBorder, CircleShape)
                        .clickable { viewModel.isBalanceHidden.value = !isBalanceHidden },
                    contentAlignment = Alignment.Center
                ) {
                    AureumVectorIcon(
                        name = if (isBalanceHidden) "visibility_off" else "visibility",
                        tint = specs.contentPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // 2. Balance Header ("My Balance" & Total Amount)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.isBalanceHidden.value = !isBalanceHidden }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "My Balance",
                    color = specs.contentSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBalanceHidden) "••••••••" else "$currency${String.format(Locale.getDefault(), "%,.2f", totalNetWorth)}",
                    color = specs.contentPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // 3. Vaults Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VAULT ACCOUNTS",
                    color = specs.contentSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // 4. Swipeable Vault Accounts Card Carousel (Right & Left Swipeable with custom colors)
        item {
            HorizontalPager(
                state = cardPagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 14.dp
            ) { page ->
                val card = vaultCardList[page]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(card.gradient))
                        .clickable {
                            if (card.account != null) {
                                selectedDetailVault = card.account
                            } else {
                                onAddTransactionClick()
                            }
                        }
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Row: Vault Icon + Vault Name & Vault Tag Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(card.textColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AureumVectorIcon(
                                        name = card.iconName,
                                        tint = card.textColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = card.name,
                                        color = card.textColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = card.tag,
                                        color = card.subTextColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            // If this is an actual vault account, show quick edit button on top right too
                            if (card.account != null) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(card.textColor.copy(alpha = 0.15f))
                                        .clickable {
                                            val acc = card.account
                                            editingVaultAccount = acc
                                            editVaultName = acc.name
                                            editVaultBalance = acc.balance.toString()
                                            editVaultType = acc.type.uppercase()
                                            editVaultCreditLimit = acc.creditLimit?.toString() ?: ""
                                            editVaultColor = acc.tintHex
                                            showEditVaultDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AureumVectorIcon(
                                        name = "edit",
                                        tint = card.textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Row: Balance & Interactive Pill Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = if (card.dueAmount > 0) "Total due" else "Vault Balance",
                                    color = card.subTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val displayVal = if (card.dueAmount > 0) card.dueAmount else card.balance
                                Text(
                                    text = if (isBalanceHidden) "••••" else "$currency${String.format(Locale.getDefault(), "%,.2f", displayVal)}",
                                    color = card.textColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Interactive Pill Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(card.buttonBg)
                                    .clickable {
                                        if (card.account != null) {
                                            val acc = card.account
                                            editingVaultAccount = acc
                                            editVaultName = acc.name
                                            editVaultBalance = acc.balance.toString()
                                            editVaultType = acc.type.uppercase()
                                            editVaultCreditLimit = acc.creditLimit?.toString() ?: ""
                                            editVaultColor = acc.tintHex
                                            showEditVaultDialog = true
                                        } else {
                                            onAddTransactionClick()
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = card.buttonLabel,
                                    color = card.buttonTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Dynamic Carousel Dots Indicator (Synced with Active Swipe Page)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(cardPagerState.pageCount) { index ->
                    val isSelected = cardPagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 16.dp else 5.dp,
                        label = "carousel_dot_width"
                    )
                    val dotColor = if (isSelected) {
                        if (isDark) NeonLime else CardBlack
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFD1D5DB)
                    }

                    Box(
                        modifier = Modifier
                            .size(width = dotWidth, height = 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(dotColor)
                    )
                    if (index < cardPagerState.pageCount - 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }

        // 5. Recent Activity Section (Directly below swipeable cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.activeTab.value = "TRANSACTIONS" },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent activity",
                        color = specs.contentPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    AureumVectorIcon(
                        name = "chevron_right",
                        tint = specs.contentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                val limitTransList = transactions.take(4)
                if (limitTransList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(primaryCardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent activity yet.", color = specs.contentSecondary, fontSize = 13.sp)
                    }
                } else {
                    limitTransList.forEach { trans ->
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
            confirmButton = {},
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val acc = selectedDetailVault
                            if (acc != null) {
                                editingVaultAccount = acc
                                editVaultName = acc.name
                                editVaultBalance = acc.balance.toString()
                                editVaultType = acc.type.uppercase()
                                editVaultCreditLimit = acc.creditLimit?.toString() ?: ""
                                editVaultColor = acc.tintHex
                                selectedDetailVault = null
                                showEditVaultDialog = true
                            }
                        }
                    ) {
                        Text("Edit Vault & Color", color = specs.activeContainer, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { selectedDetailVault = null }) {
                        Text("Close", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // Add Vault Dialog with Card Color Selector on Home Screen
    if (showAddVaultDialog) {
        AlertDialog(
            onDismissRequest = { showAddVaultDialog = false },
            title = { Text("Add New Vault Account", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TextField(
                        value = vaultNameInput,
                        onValueChange = { vaultNameInput = it },
                        label = { Text("Vault Name (e.g., Cash, Card, Bank)") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    )
                    TextField(
                        value = vaultBalanceInput,
                        onValueChange = { vaultBalanceInput = it },
                        label = { Text("Starting Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    )

                    Text("VAULT TYPE", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "ACCOUNTS", "CARD").forEach { t ->
                            val active = vaultTypeInput == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) specs.activeContainer else specs.inactiveContainer)
                                    .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                                    .clickable { 
                                        vaultTypeInput = t 
                                        if (t == "CASH" && vaultColorInput == "#10B981") vaultColorInput = "#10B981"
                                        if (t == "CARD" && vaultColorInput == "#10B981") vaultColorInput = "#3B82F6"
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (t == "ACCOUNTS") "BANK" else t,
                                    fontSize = 11.sp,
                                    color = if (active) specs.onActiveContainer else specs.contentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (vaultTypeInput == "CARD" || vaultTypeInput == "CREDIT") {
                        TextField(
                            value = vaultCreditLimitInput,
                            onValueChange = { vaultCreditLimitInput = it },
                            label = { Text("Credit Limit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = specs.contentPrimary,
                                unfocusedTextColor = specs.contentPrimary,
                                focusedContainerColor = specs.inactiveContainer,
                                unfocusedContainerColor = specs.inactiveContainer
                            ),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        )
                    }

                    // Card Color Picker
                    Text("CARD COLOR THEME", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        vaultPresetColors.forEach { (hex, name) ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = vaultColorInput.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) specs.contentPrimary else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { vaultColorInput = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    AureumVectorIcon(
                                        name = "check",
                                        tint = if (hex == "#B0FF27") Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vaultNameInput.isNotBlank()) {
                            val limit = vaultCreditLimitInput.toDoubleOrNull()
                            viewModel.addAccount(
                                name = vaultNameInput.trim(),
                                initialBalance = vaultBalanceInput.toDoubleOrNull() ?: 0.0,
                                type = vaultTypeInput,
                                creditLimit = limit,
                                tintHex = vaultColorInput
                            )
                            vaultNameInput = ""
                            vaultBalanceInput = ""
                            vaultCreditLimitInput = ""
                            showAddVaultDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Vault", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVaultDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Edit Vault Dialog with Card Color Selector on Home Screen
    if (showEditVaultDialog && editingVaultAccount != null) {
        val targetAcc = editingVaultAccount!!
        AlertDialog(
            onDismissRequest = { 
                showEditVaultDialog = false 
                editingVaultAccount = null
            },
            title = { Text("Edit Vault & Card Color", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TextField(
                        value = editVaultName,
                        onValueChange = { editVaultName = it },
                        label = { Text("Vault Name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    )
                    TextField(
                        value = editVaultBalance,
                        onValueChange = { editVaultBalance = it },
                        label = { Text("Current Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = specs.contentPrimary,
                            unfocusedTextColor = specs.contentPrimary,
                            focusedContainerColor = specs.inactiveContainer,
                            unfocusedContainerColor = specs.inactiveContainer
                        ),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    )

                    Text("VAULT TYPE", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "ACCOUNTS", "CARD").forEach { t ->
                            val active = editVaultType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) specs.activeContainer else specs.inactiveContainer)
                                    .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                                    .clickable { editVaultType = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (t == "ACCOUNTS") "BANK" else t,
                                    fontSize = 11.sp,
                                    color = if (active) specs.onActiveContainer else specs.contentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (editVaultType == "CARD" || editVaultType == "CREDIT") {
                        TextField(
                            value = editVaultCreditLimit,
                            onValueChange = { editVaultCreditLimit = it },
                            label = { Text("Credit Limit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = specs.contentPrimary,
                                unfocusedTextColor = specs.contentPrimary,
                                focusedContainerColor = specs.inactiveContainer,
                                unfocusedContainerColor = specs.inactiveContainer
                            ),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        )
                    }

                    // Card Color Picker
                    Text("CARD COLOR THEME", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        vaultPresetColors.forEach { (hex, name) ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = editVaultColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) specs.contentPrimary else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { editVaultColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    AureumVectorIcon(
                                        name = "check",
                                        tint = if (hex == "#B0FF27") Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (accounts.size > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.deleteAccount(targetAcc)
                                showEditVaultDialog = false
                                editingVaultAccount = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("DELETE THIS VAULT", color = ExpenseCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = editVaultCreditLimit.toDoubleOrNull()
                        val updated = targetAcc.copy(
                            name = editVaultName.trim(),
                            balance = editVaultBalance.toDoubleOrNull() ?: targetAcc.balance,
                            type = editVaultType,
                            creditLimit = limit,
                            tintHex = editVaultColor,
                            iconName = when (editVaultType) {
                                "ACCOUNTS", "BANK" -> "account_balance"
                                "CASH" -> "savings"
                                "CARD", "CREDIT" -> "credit_card"
                                else -> "account_balance"
                            }
                        )
                        viewModel.updateAccount(updated)
                        showEditVaultDialog = false
                        editingVaultAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditVaultDialog = false 
                    editingVaultAccount = null
                }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
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
                    .fillMaxWidth(0.9f)
                    .graphicsLayer {
                        scaleX = popupScale
                        scaleY = popupScale
                        this.alpha = popupAlpha
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (specs.isDark) Color(0xFF1E2430) else Color.White)
                    .border(1.dp, if (specs.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                    .padding(22.dp)
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
    val timeFilter by viewModel.transactionTimeFilter.collectAsState()
    val customMonth by viewModel.transactionCustomMonth.collectAsState()
    val customYear by viewModel.transactionCustomYear.collectAsState()

    var showMonthPickerDialog by remember { mutableStateOf(false) }

    // Aggregate category types for filter
    val categoriesList = listOf("ALL") + transactions.map { it.category }.distinct()

    val monthNames = remember {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
    val shortMonthNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    val filteredList = remember(transactions, categoryFilter, filterType, timeFilter, customMonth, customYear) {
        transactions.filter {
            val categoryMatch = categoryFilter == "ALL" || it.category == categoryFilter
            val typeMatch = when (filterType) {
                "EXPENSES" -> it.type == "EXPENSE"
                "INCOMES" -> it.type == "INCOME"
                else -> true
            }
            val timeMatch = when (timeFilter) {
                "TODAY" -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    it.timestamp >= cal.timeInMillis
                }
                "7_DAYS" -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_YEAR, -6) // 7 days including today
                    }
                    it.timestamp >= cal.timeInMillis
                }
                "15_DAYS" -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_YEAR, -14) // 15 days including today
                    }
                    it.timestamp >= cal.timeInMillis
                }
                "30_DAYS" -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_YEAR, -29) // 30 days including today
                    }
                    it.timestamp >= cal.timeInMillis
                }
                "THIS_MONTH" -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startOfMonth = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    val endOfMonth = cal.timeInMillis
                    it.timestamp in startOfMonth until endOfMonth
                }
                "CUSTOM_MONTH" -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, customYear)
                        set(Calendar.MONTH, customMonth)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startOfCustom = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    val endOfCustom = cal.timeInMillis
                    it.timestamp in startOfCustom until endOfCustom
                }
                else -> true
            }
            categoryMatch && typeMatch && timeMatch
        }.sortedByDescending { it.timestamp }
    }

    val periodIncome = remember(filteredList) {
        filteredList.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val periodExpense = remember(filteredList) {
        filteredList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    val specs = AureumThemeStyles.specs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LEDGER ARCHIVE",
                color = specs.contentSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "${filteredList.size} records",
                color = specs.contentSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 1. Type Filters (ALL / EXPENSES / INCOMES)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        .padding(vertical = 9.dp),
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

        // 2. Time Filters Horizontal Scroll (Today, Last 7 Days, 15 Days, 30 Days, This Month, Select Month, All Time)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val timeFilterOptions = listOf(
                "TODAY" to "Today",
                "7_DAYS" to "Last 7 Days",
                "15_DAYS" to "15 Days",
                "30_DAYS" to "30 Days",
                "THIS_MONTH" to "This Month",
                "ALL" to "All Time"
            )

            timeFilterOptions.forEach { (key, label) ->
                val active = timeFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) specs.activeContainer else specs.inactiveContainer)
                        .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.transactionTimeFilter.value = key }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        color = if (active) specs.onActiveContainer else specs.contentPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Custom Month Selector Chip
            val isCustomMonthActive = timeFilter == "CUSTOM_MONTH"
            val customMonthLabel = if (isCustomMonthActive) {
                "${shortMonthNames.getOrElse(customMonth) { "Month" }} $customYear"
            } else {
                "Select Month..."
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCustomMonthActive) specs.activeContainer else specs.inactiveContainer)
                    .border(1.dp, if (isCustomMonthActive) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                    .clickable { showMonthPickerDialog = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AureumVectorIcon(
                        name = "calendar",
                        tint = if (isCustomMonthActive) specs.onActiveContainer else specs.contentPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = customMonthLabel,
                        color = if (isCustomMonthActive) specs.onActiveContainer else specs.contentPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isCustomMonthActive) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // 3. Category Filter Scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (active) activeText else inactiveText,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // 4. Period Summary Box (Income / Expense Total for Selected Filter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(specs.inactiveContainer)
                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("In:", color = specs.contentSecondary, fontSize = 11.sp)
                Text(
                    "+$currency${String.format(Locale.getDefault(), "%,.2f", periodIncome)}",
                    color = AssetGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Out:", color = specs.contentSecondary, fontSize = 11.sp)
                Text(
                    "-$currency${String.format(Locale.getDefault(), "%,.2f", periodExpense)}",
                    color = ExpenseCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val netDiff = periodIncome - periodExpense
                Text("Net:", color = specs.contentSecondary, fontSize = 11.sp)
                Text(
                    "${if (netDiff >= 0) "+" else ""}$currency${String.format(Locale.getDefault(), "%,.2f", netDiff)}",
                    color = if (netDiff >= 0) specs.contentPrimary else ExpenseCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 5. Expense distribution Animated Pie Chart visualization (Synced to filteredList)
        ExpenseDistributionPieChart(transactions = filteredList, viewModel = viewModel)

        // 6. Chronological Transaction List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        Text("No matching transactions in this time period.", color = specs.contentSecondary, fontSize = 13.sp)
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

    // Month & Year Picker Dialog
    if (showMonthPickerDialog) {
        var pickerYear by remember { mutableStateOf(customYear) }
        var pickerMonth by remember { mutableStateOf(customMonth) }

        AlertDialog(
            onDismissRequest = { showMonthPickerDialog = false },
            title = {
                Text("Select Month & Year", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Year Navigator Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(specs.inactiveContainer)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { pickerYear-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            AureumVectorIcon(name = "chevron_left", tint = specs.contentPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = pickerYear.toString(),
                            color = specs.contentPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { pickerYear++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            AureumVectorIcon(name = "chevron_right", tint = specs.contentPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // 12 Months Grid (3 columns x 4 rows)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val mIndex = row * 3 + col
                                    val isSelected = pickerMonth == mIndex
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) specs.activeContainer else specs.inactiveContainer)
                                            .border(1.dp, if (isSelected) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(8.dp))
                                            .clickable { pickerMonth = mIndex }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortMonthNames[mIndex],
                                            color = if (isSelected) specs.onActiveContainer else specs.contentPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.transactionCustomYear.value = pickerYear
                        viewModel.transactionCustomMonth.value = pickerMonth
                        viewModel.transactionTimeFilter.value = "CUSTOM_MONTH"
                        showMonthPickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Filter", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMonthPickerDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
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
// ANALYTICS / STATS SCREEN (Growth & Watch List)
// -------------------------------------------------------------
@Composable
fun StatsScreen(
    viewModel: AureumViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val currency by viewModel.selectedCurrency.collectAsState()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsState()

    val totalExpenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalIncomes = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val specs = AureumThemeStyles.specs
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val primaryCardBg = if (isDark) SurfaceContainer else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEFF1F5)

    // Map spend distributions
    val categoriesSpend = remember(transactions) {
        val map = mutableMapOf<String, Double>()
        transactions.filter { it.type == "EXPENSE" }.forEach {
            map[it.category] = (map[it.category] ?: 0.0) + it.amount
        }
        val sortedList = map.toList().sortedByDescending { it.second }.take(3)
        val colors = listOf(ChequingBlue, SavingsGreen, CryptoPink)
        
        sortedList.mapIndexed { idx, pair ->
            PieSegment(category = pair.first, value = pair.second, color = colors.getOrElse(idx) { ChequingBlue })
        }
    }

    val topCategory = categoriesSpend.firstOrNull()?.category ?: "None"

    val trendValues = remember(transactions) {
        val raw = transactions.filter { it.type == "EXPENSE" }.take(7).map { it.amount.toFloat() }.reversed()
        if (raw.isEmpty()) listOf(0f, 0f, 0f) else raw
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        // 1. Top App Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: User Profile Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE0E7FF), Color(0xFFC7D2FE))
                            )
                        )
                        .border(1.5.dp, if (isDark) NeonLime.copy(alpha = 0.6f) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        color = Color(0xFF3730A3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center: Screen Title
                Text(
                    text = "Wallet",
                    color = specs.contentPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Right: Notification Bell
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primaryCardBg)
                        .border(1.dp, cardBorder, CircleShape)
                        .clickable { /* Notification tap */ },
                    contentAlignment = Alignment.Center
                ) {
                    AureumVectorIcon(
                        name = "bell",
                        tint = specs.contentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Hero Portfolio Growth Card (Matching Right Phone UI)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(primaryCardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row: Trend Icon + Portfolio growth + Last 7 days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                AureumVectorIcon(
                                    name = "stats",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Portfolio growth",
                                color = specs.contentPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Last 7 days",
                            color = specs.contentSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Content Row: Total Growth on Left, 7-Day Bar Chart on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Total growth & percentage
                        Column {
                            Text(
                                text = "Total growth",
                                color = specs.contentSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val growthVal = if (totalIncomes > 0) totalIncomes else 4756.67
                                Text(
                                    text = if (isBalanceHidden) "••••" else "$currency${String.format(Locale.getDefault(), "%,.2f", growthVal)}",
                                    color = specs.contentPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(+6.2%)",
                                    color = Color(0xFF16A34A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 7-day mini bar chart matching reference image
                        val sampleDays = listOf(
                            Pair("T", -14),
                            Pair("W", 28),
                            Pair("T", 38),
                            Pair("F", -18),
                            Pair("S", 30),
                            Pair("S", 45),
                            Pair("M", 34)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            sampleDays.forEach { (day, valPct) ->
                                val isPositive = valPct >= 0
                                val barHeight = (kotlin.math.abs(valPct) * 0.9f).coerceIn(12f, 48f)
                                val barColor = if (isPositive) NeonLime else ExpenseCoral

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(7.dp)
                                            .height(barHeight.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(barColor)
                                    )
                                    Text(
                                        text = day,
                                        color = specs.contentSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Watch list Section (Exact Match to Right Phone UI)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Watch list",
                        color = specs.contentPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    AureumVectorIcon(
                        name = "chevron_right",
                        tint = specs.contentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Reference watchlist assets: Bitcoin, Ethereum, Solana, USD Coin, Veritaseum
                val watchListItems = listOf(
                    WatchAsset("Bitcoin", "BTC", "$83,241 USD", "+$1,204.50 (+1.47%)", true, BitcoinOrange, "btc"),
                    WatchAsset("Ethereum", "ETH", "$1,934.20 USD", "+$84.10 (+4.55%)", true, EthereumSlate, "eth"),
                    WatchAsset("Solana", "SOL", "$128.40 USD", "-$9.32 (-6.77%)", false, SolanaPurple, "sol"),
                    WatchAsset("USD Coin", "USDC", "$1.00 USD", "+$0.001 (+0.10%)", true, UsdcBlue, "usdc"),
                    WatchAsset("Veritaseum", "VRI", "$11.68 USD", "+$0.03 (+9.6%)", true, VeritaseumGold, "hexagon_gear")
                )

                watchListItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(primaryCardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Badge & Name/Symbol
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(item.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AureumVectorIcon(
                                        name = item.icon,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = item.name,
                                        color = specs.contentPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = item.symbol,
                                        color = specs.contentSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Right: Price & Change
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = item.price,
                                    color = specs.contentPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.change,
                                    color = if (item.isPositive) Color(0xFF16A34A) else ExpenseCoral,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Bento Inflow / Outflow Cards
        item {
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
                        Text("Total Outflow", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$currency${String.format(Locale.getDefault(), "%,.0f", totalExpenses)}",
                            color = ExpenseCoral,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
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
                        Text("Total Inputs", color = specs.contentSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$currency${String.format(Locale.getDefault(), "%,.0f", totalIncomes)}",
                            color = if (isDark) AssetGreen else Color(0xFF2E7D32),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Category Breakdown Donut Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(primaryCardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DonutChart(
                        segments = categoriesSpend,
                        modifier = Modifier.size(110.dp),
                        centerTextLabel = "TOP",
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
        }
    }
}

private data class WatchAsset(
    val name: String,
    val symbol: String,
    val price: String,
    val change: String,
    val isPositive: Boolean,
    val color: Color,
    val icon: String
)


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
    var addAccountColor by remember { mutableStateOf("#10B981") }

    // Edit Account Dialog states
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountBalance by remember { mutableStateOf("") }
    var editAccountType by remember { mutableStateOf("ACCOUNTS") }
    var editCreditLimit by remember { mutableStateOf("") }
    var editAccountColor by remember { mutableStateOf("#10B981") }

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

                    // Card Color Picker
                    Text("CARD COLOR THEME", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        vaultPresetColors.forEach { (hex, name) ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = addAccountColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) specs.contentPrimary else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { addAccountColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    AureumVectorIcon(
                                        name = "check",
                                        tint = if (hex == "#B0FF27") Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
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
                            tintHex = addAccountColor
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

                    // Card Color Picker
                    Text("CARD COLOR THEME", color = specs.contentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        vaultPresetColors.forEach { (hex, name) ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = editAccountColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) specs.contentPrimary else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { editAccountColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    AureumVectorIcon(
                                        name = "check",
                                        tint = if (hex == "#B0FF27") Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
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
                            tintHex = editAccountColor,
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
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

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
            selectedTimestamp = tx.timestamp
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
            .padding(top = 12.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (editingTx != null) "EDIT TRANSACTION" else "NEW TRANSACTION",
                color = specs.contentPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = specs.inactiveContainer
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
            ) {
                Text("Dismiss", color = specs.contentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. Mode switch (Expense / Income / Transfer)
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
                        .height(46.dp)
                        .then(
                            if (!active) Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp)) else Modifier
                        ),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) specs.activeContainer else specs.inactiveContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        t,
                        fontSize = 12.sp,
                        color = if (active) specs.onActiveContainer else specs.contentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Description / Memo Field
        TextField(
            value = noteStr,
            onValueChange = { noteStr = it },
            placeholder = { Text("What is this transaction for? (Description)", color = specs.contentSecondary, fontSize = 13.sp) },
            singleLine = true,
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
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
        )

        // 4. Amount Hero Input Box (Spacious, prominent)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(specs.inactiveContainer)
                .border(1.dp, specs.activeContainer.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable {
                    keypadTarget = "AMOUNT"
                    showKeyboard = true
                }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Amount",
                        color = specs.contentSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$currency$amountStr",
                        color = if (selectedType == "INCOME") AssetGreen else if (selectedType == "EXPENSE") ExpenseCoral else specs.contentPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(specs.activeContainer.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Keypad",
                        color = specs.activeContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    AureumVectorIcon(name = "edit", tint = specs.activeContainer, modifier = Modifier.size(15.dp))
                }
            }
        }

        // 5. Date & Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
            val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

            // Date Button
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(specs.inactiveContainer)
                    .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedTimestamp
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                selectedTimestamp = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AureumVectorIcon(name = "calendar", tint = specs.activeContainer, modifier = Modifier.size(18.dp))
                    Text(
                        dateFormatter.format(Date(selectedTimestamp)),
                        color = specs.contentPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Time Button
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(specs.inactiveContainer)
                    .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedTimestamp
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                }
                                selectedTimestamp = newCal.timeInMillis
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AureumVectorIcon(name = "clock", tint = specs.activeContainer, modifier = Modifier.size(18.dp))
                    Text(
                        timeFormatter.format(Date(selectedTimestamp)),
                        color = specs.contentPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 6. Category Selection Section
        if (selectedType != "TRANSFER") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CATEGORY",
                    color = specs.contentSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
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
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AureumVectorIcon(
                                    name = cat.icon,
                                    tint = if (active) specs.onActiveContainer else specs.contentPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    cat.name,
                                    color = if (active) specs.onActiveContainer else specs.contentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Source Account Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (selectedType == "TRANSFER") "FROM VAULT" else "PAYMENT / VAULT ACCOUNT",
                color = specs.contentSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
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
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            acc.name,
                            color = if (active) specs.onActiveContainer else specs.contentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 8. Destination Account & Transfer Fee (For Transfers)
        if (selectedType == "TRANSFER") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TO VAULT",
                    color = specs.contentSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                        val active = destinationAccountId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) specs.activeContainer else specs.inactiveContainer)
                                .border(1.dp, if (active) Color.Transparent else specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                                .clickable { destinationAccountId = acc.id }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                acc.name,
                                color = if (active) specs.onActiveContainer else specs.contentPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Transfer Fee Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(specs.inactiveContainer)
                    .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        keypadTarget = "FEE"
                        showKeyboard = true
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transfer Fee (Optional):", color = specs.contentSecondary, fontSize = 12.sp)
                    Text("$currency$transferFeeStr", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // 9. Attach Receipt Image
        if (receiptPath == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(specs.inactiveContainer)
                    .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AureumVectorIcon(name = "image", tint = specs.activeContainer, modifier = Modifier.size(18.dp))
                    Text("Attach Receipt / Voucher Image (Optional)", color = specs.contentSecondary, fontSize = 12.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(specs.inactiveContainer)
                    .border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = receiptPath,
                            contentDescription = "Picked Receipt Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Text("Receipt Attached", color = specs.contentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = { receiptPath = null },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Image",
                        tint = ExpenseCoral,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 10. Log / Update Transaction Button
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
                            timestamp = selectedTimestamp,
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
                            timestamp = selectedTimestamp,
                            receiptPath = receiptPath,
                            fee = feeVal
                        )
                    }
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (editingTx != null) "UPDATE TRANSACTION" else "LOG TRANSACTION",
                color = specs.onActiveContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
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
                            .hapticClickable(
                                style = if (char == "⌫") HapticStyle.HEAVY else HapticStyle.LIGHT
                            ) { onKeyPress(char) },
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val specs = AureumThemeStyles.specs

    val currency by viewModel.selectedCurrency.collectAsState()
    val themeOverride by viewModel.isDarkTheme.collectAsState()
    val exportStatusMsg by viewModel.exportMessage.collectAsState()
    val secureMode by viewModel.secureModeEnabled.collectAsState()

    val isPinGuardEnabled by viewModel.isPinGuardEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val correctPin by viewModel.correctPin.collectAsState()
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    var showWipeDataConfirm by remember { mutableStateOf(false) }
    var wipeDataPinInput by remember { mutableStateOf("") }
    var wipeDataError by remember { mutableStateOf(false) }

    var showCategoryManager by remember { mutableStateOf(false) }
    var showRestoreDefaultsConfirm by remember { mutableStateOf(false) }

    // Backup & Restore State
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var exportTransactions by remember { mutableStateOf(true) }
    var exportImages by remember { mutableStateOf(true) }
    var exportAccounts by remember { mutableStateOf(true) }
    var exportCategories by remember { mutableStateOf(true) }
    var exportLoans by remember { mutableStateOf(true) }
    var restoreReportMsg by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val json = viewModel.generateBackupJsonString(
                    includeTransactions = exportTransactions,
                    includeImages = exportImages,
                    includeAccounts = exportAccounts,
                    includeCategories = exportCategories,
                    includeLoans = exportLoans
                )
                val success = viewModel.writeJsonToUri(context, uri, json)
                if (success) {
                    android.widget.Toast.makeText(context, "Backup exported successfully to JSON file!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to save backup file.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val json = viewModel.readJsonFromUri(context, uri)
                if (json != null) {
                    val report = viewModel.importBackupFromJsonString(context, json)
                    restoreReportMsg = report
                } else {
                    android.widget.Toast.makeText(context, "Failed to read selected JSON file.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                    Text("Biometric / Fingerprint Sensor", color = specs.contentPrimary, fontSize = 14.sp)
                    Text("Unlock app using fingerprint or face", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = specs.activeContainer)
                )
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

        // Backup & Restore Engine (JSON & CSV)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("DATA BACKUP & RESTORATION", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Create full or modular offline backups, export CSV sheets, or restore your data.", color = specs.contentSecondary, fontSize = 11.sp)
            
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Modular JSON Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Modular JSON Backup", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Export accounts, transactions, categories, and loans", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { showExportOptionsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    AureumVectorIcon(name = "file_download", tint = specs.onActiveContainer, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", color = specs.onActiveContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(specs.inactiveContainerBorder))
            Spacer(modifier = Modifier.height(12.dp))

            // 2. JSON Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Import Backup File", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Restore data from a previously exported .json file", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { importJsonLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.inactiveContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                ) {
                    AureumVectorIcon(name = "file_upload", tint = specs.contentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(specs.inactiveContainerBorder))
            Spacer(modifier = Modifier.height(12.dp))

            // 3. CSV Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CSV Offline Ledger Export", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Generates clean CSV spreadsheet of transactions", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { viewModel.exportLedgerToCsv() },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.inactiveContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, specs.inactiveContainerBorder, RoundedCornerShape(10.dp))
                ) {
                    Text("CSV", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

        // App Version & Status Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Aureum Wallet", color = specs.contentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Version 2.0.0 (Build 3) • 100% Offline", color = specs.contentSecondary, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(specs.activeContainer.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("v2.0.0", color = specs.activeContainer, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }

    // Modular JSON Export Options Dialog
    if (showExportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = {
                Text("Select Data to Export", color = specs.contentPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Choose which data components you want to include in your offline JSON backup file:",
                        color = specs.contentSecondary,
                        fontSize = 12.sp
                    )

                    // 1. Transactions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(specs.inactiveContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transactions & Logs", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("All income, expense, and transfers", color = specs.contentSecondary, fontSize = 10.sp)
                        }
                        Checkbox(
                            checked = exportTransactions,
                            onCheckedChange = { exportTransactions = it },
                            colors = CheckboxDefaults.colors(checkedColor = specs.activeContainer)
                        )
                    }

                    // 1.1 Include Images (Indented if transactions selected)
                    if (exportTransactions) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(specs.inactiveContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Include Receipt Images", color = specs.contentPrimary, fontSize = 12.sp)
                                Text("Embedded inside JSON as Base64", color = specs.contentSecondary, fontSize = 9.sp)
                            }
                            Checkbox(
                                checked = exportImages,
                                onCheckedChange = { exportImages = it },
                                colors = CheckboxDefaults.colors(checkedColor = specs.activeContainer)
                            )
                        }
                    }

                    // 2. Accounts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(specs.inactiveContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vault Accounts & Cards", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Custom accounts, balances, credit limits", color = specs.contentSecondary, fontSize = 10.sp)
                        }
                        Checkbox(
                            checked = exportAccounts,
                            onCheckedChange = { exportAccounts = it },
                            colors = CheckboxDefaults.colors(checkedColor = specs.activeContainer)
                        )
                    }

                    // 3. Categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(specs.inactiveContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Categories & Display Order", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Custom category icons and ordering", color = specs.contentSecondary, fontSize = 10.sp)
                        }
                        Checkbox(
                            checked = exportCategories,
                            onCheckedChange = { exportCategories = it },
                            colors = CheckboxDefaults.colors(checkedColor = specs.activeContainer)
                        )
                    }

                    // 4. Loans & Profiles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(specs.inactiveContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Loan Channels & Profiles", color = specs.contentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Soft loans, formal facilities, schedules", color = specs.contentSecondary, fontSize = 10.sp)
                        }
                        Checkbox(
                            checked = exportLoans,
                            onCheckedChange = { exportLoans = it },
                            colors = CheckboxDefaults.colors(checkedColor = specs.activeContainer)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportOptionsDialog = false
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportJsonLauncher.launch("aureum_backup_$timestamp.json")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Export JSON", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportOptionsDialog = false }) {
                    Text("Cancel", color = specs.contentSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Restore Report Bottom Sheet Alert
    if (restoreReportMsg != null) {
        AlertDialog(
            onDismissRequest = { restoreReportMsg = null },
            title = { Text("Backup Restoration Report", color = specs.contentPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = restoreReportMsg ?: "",
                    color = specs.contentPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { restoreReportMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = specs.activeContainer)
                ) {
                    Text("Done", color = specs.onActiveContainer, fontWeight = FontWeight.Bold)
                }
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
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Left Side: Reorder Handle & Quick Move Controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.moveCategoryUp(cat) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        AureumVectorIcon(name = "arrow_up", tint = specs.contentSecondary, modifier = Modifier.size(13.dp))
                                    }
                                    AureumVectorIcon(
                                        name = "drag_handle",
                                        tint = specs.contentSecondary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.moveCategoryDown(cat) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        AureumVectorIcon(name = "arrow_down", tint = specs.contentSecondary, modifier = Modifier.size(13.dp))
                                    }
                                }

                                // Category Icon
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(specs.activeContainer.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AureumVectorIcon(name = cat.icon, tint = specs.contentPrimary, modifier = Modifier.size(17.dp))
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
