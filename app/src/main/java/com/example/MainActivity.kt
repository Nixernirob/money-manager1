package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AureumVectorIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.AureumViewModel
import com.example.ui.screens.*

class MainActivity : FragmentActivity() {
    private val viewModel: AureumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val secureMode by viewModel.secureModeEnabled.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

            // Dynamic secure Flag integration
            LaunchedEffect(secureMode) {
                if (secureMode) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Trigger biometric prompt on lock if enabled
            LaunchedEffect(isLocked) {
                if (isLocked && isBiometricEnabled) {
                    launchBiometricUnlock()
                }
            }

            AureumTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aureumBackground()
                ) {
                    // Flow Navigation switch frame
                    MainApplicationChrome(viewModel = viewModel)

                    // Unified Lock screen guard overlay
                    AnimatedVisibility(
                        visible = isLocked,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        AppLockscreenOverlay(
                            viewModel = viewModel,
                            onBiometricClick = { launchBiometricUnlock() }
                        )
                    }
                }
            }
        }
    }

    fun launchBiometricUnlock() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockApp()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User cancelled or fallback to PIN
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Aureum")
            .setSubtitle("Authenticate using your fingerprint or screen lock")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainApplicationChrome(viewModel: AureumViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    
    // Track popup creators overlay
    var showTransactionCreator by remember { mutableStateOf(false) }

    val editingTx by viewModel.editingTransaction.collectAsState()
    LaunchedEffect(editingTx) {
        if (editingTx != null) {
            showTransactionCreator = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            // High visibility floating docking navigation bar
            NavigationBarCustomDock(
                activeTab = activeTab,
                onTabSelect = { tabId ->
                    // Automatically close transaction sheet if open when tapping nav bar
                    showTransactionCreator = false
                    viewModel.editingTransaction.value = null
                    viewModel.activeTab.value = tabId
                },
                onCenterAdd = { showTransactionCreator = true }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring(dampingRatio = 0.8f)) togetherWith
                    fadeOut(animationSpec = spring(dampingRatio = 0.8f))
                },
                label = "MainTabsNavigationTransition"
            ) { targetTab ->
                when (targetTab) {
                    "HOME" -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onAddTransactionClick = { showTransactionCreator = true }
                        )
                    }
                    "TRANSACTIONS" -> {
                        TransactionsScreen(viewModel = viewModel)
                    }
                    "STATS" -> {
                        StatsScreen(viewModel = viewModel)
                    }
                    "LOANS" -> {
                        LoansScreen(viewModel = viewModel)
                    }
                    "ACCOUNTS" -> {
                        AccountsScreen(viewModel = viewModel)
                    }
                    "MORE" -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
            
            // Transaction creator sheet launcher
            AnimatedVisibility(
                visible = showTransactionCreator,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AddTransactionScreen(
                        viewModel = viewModel,
                        onComplete = {
                            showTransactionCreator = false
                            viewModel.editingTransaction.value = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationBarCustomDock(
    activeTab: String,
    onTabSelect: (String) -> Unit,
    onCenterAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val specs = AureumThemeStyles.specs

    val dockBackground = if (isDark) Color(0xF0181C24) else Color.White
    val dockBorder = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
    val activeColor = if (isDark) NeonLime else CardBlack
    // High-contrast, high-opacity inactive icons for clear visibility
    val inactiveColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(dockBackground)
                .border(1.dp, dockBorder, RoundedCornerShape(30.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val tabs = listOf(
                TabItem("HOME", "wallet_dock", "Home"),
                TabItem("TRANSACTIONS", "receipt_long", "Ledger"),
                TabItem("ADD", "add", "Add"),
                TabItem("LOANS", "cards_dock", "Activity"),
                TabItem("MORE", "hexagon_gear", "Settings")
            )

            tabs.forEach { tab ->
                val isAdd = tab.id == "ADD"
                val active = !isAdd && (activeTab == tab.id || (tab.id == "HOME" && activeTab == "ACCOUNTS"))
                val targetColor = if (active) activeColor else inactiveColor
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { 
                            if (isAdd) {
                                onCenterAdd()
                            } else {
                                onTabSelect(tab.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isAdd) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(specs.activeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            AureumVectorIcon(
                                name = tab.icon,
                                tint = specs.onActiveContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AureumVectorIcon(
                                name = tab.icon,
                                tint = targetColor,
                                modifier = Modifier.size(22.dp)
                            )
                            if (active) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(targetColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class TabItem(val id: String, val icon: String, val label: String)

@Composable
fun AppLockscreenOverlay(
    viewModel: AureumViewModel,
    onBiometricClick: () -> Unit = {}
) {
    val pinVal by viewModel.pinInput.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val specs = AureumThemeStyles.specs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (specs.isDark) Color(0xFF0F131E) else Color(0xFFF3F5FA))
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Visual Logo Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(specs.activeContainer.copy(alpha = 0.12f))
                        .border(1.dp, specs.glassBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AureumVectorIcon(name = "vault", tint = specs.activeContainer, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AUREUM SECURE GUARD",
                    color = specs.contentPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Access protected. Enter PIN or authenticate",
                    color = specs.contentSecondary,
                    fontSize = 11.sp
                )
            }

            // PIN Dots Indicator UI
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val active = pinVal.length > i
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) specs.activeContainer else specs.inactiveContainer
                            )
                            .border(1.dp, if (active) specs.activeContainer else specs.inactiveContainerBorder, CircleShape)
                    )
                }
            }

            // Lock dialpad key inputs grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "CLR")
                ).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { d ->
                            if (d == "BIO") {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(CircleShape)
                                        .background(specs.inactiveContainer)
                                        .border(1.dp, specs.inactiveContainerBorder, CircleShape)
                                        .clickable { onBiometricClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AureumVectorIcon(
                                        name = "fingerprint",
                                        tint = specs.activeContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(CircleShape)
                                        .background(specs.inactiveContainer)
                                        .border(1.dp, specs.inactiveContainerBorder, CircleShape)
                                        .clickable {
                                            if (d == "CLR") {
                                                viewModel.clearPinInput()
                                            } else {
                                                viewModel.enterPinDigit(d)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = d,
                                        color = if (d == "CLR") ExpenseCoral else specs.contentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
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
