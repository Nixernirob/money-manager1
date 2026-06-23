package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.ui.draw.blur
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
import com.example.ui.components.AureumVectorIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.AureumViewModel
import com.example.ui.screens.*

class MainActivity : ComponentActivity() {
    private val viewModel: AureumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val secureMode by viewModel.secureModeEnabled.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            // Dynamic secure Flag integration
            LaunchedEffect(secureMode) {
                if (secureMode) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                        AppLockscreenOverlay(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainApplicationChrome(viewModel: AureumViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    
    // Track popup creators overlay
    var showTransactionCreator by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            // Elegant floating docking navigation bar custom styled
            NavigationBarCustomDock(
                activeTab = activeTab,
                onTabSelect = { viewModel.activeTab.value = it },
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
                        onComplete = { showTransactionCreator = false }
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

    val dockBackground = if (isDark) Color(0xE6141B2D) else Color.White
    val dockBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE0E2EC)
    val activeColor = if (isDark) MidnightTertiary else Color(0xFF005AC1)
    val inactiveColor = if (isDark) Color.White else Color(0xFF74777F)
    val centerButtonBg = if (isDark) MidnightTertiary else Color(0xFF005AC1)
    val centerButtonTint = if (isDark) Color.Black else Color.White

    // Guidelines command: Use safe drawing system navigation bar safe padding offset
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(dockBackground)
                .border(1.dp, dockBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                TabItem("HOME", "home", "Dashboard"),
                TabItem("TRANSACTIONS", "transfer", "Ledger"),
                TabItem("STATS", "stats", "Analytics")
            ).forEach { tab ->
                val active = activeTab == tab.id
                val alphaState by animateFloatAsState(targetValue = if (active) 1f else 0.45f)
                val targetColor = if (active) activeColor else inactiveColor.copy(alpha = alphaState)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelect(tab.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AureumVectorIcon(
                            name = tab.icon,
                            tint = targetColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            color = targetColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Central add interactive key launcher
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(centerButtonBg)
                    .clickable { onCenterAdd() },
                contentAlignment = Alignment.Center
            ) {
                AureumVectorIcon(name = "add", tint = centerButtonTint, modifier = Modifier.size(20.dp))
            }

            listOf(
                TabItem("ACCOUNTS", "bank", "Vaults"),
                TabItem("MORE", "settings", "Preferences")
            ).forEach { tab ->
                val active = activeTab == tab.id
                val alphaState by animateFloatAsState(targetValue = if (active) 1f else 0.45f)
                val targetColor = if (active) activeColor else inactiveColor.copy(alpha = alphaState)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelect(tab.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AureumVectorIcon(
                            name = tab.icon,
                            tint = targetColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            color = targetColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class TabItem(val id: String, val icon: String, val label: String)

@Composable
fun AppLockscreenOverlay(viewModel: AureumViewModel) {
    val pinVal by viewModel.pinInput.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaseMidnightOffset)
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
                        .background(MidnightTertiary.copy(alpha = 0.1f))
                        .border(1.dp, MidnightTertiary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AureumVectorIcon(name = "vault", tint = MidnightTertiary, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AUREUM SECURE GUARD",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Access protected. Please insert security code",
                    color = TextSecondary,
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
                                if (active) MidnightTertiary else Color.White.copy(alpha = 0.1f)
                            )
                            .border(1.dp, if (active) MidnightTertiary else Color.Transparent, CircleShape)
                    )
                }
            }

            // Lock dialpad key inputs grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "CLR")
                ).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { d ->
                            if (d.isBlank()) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
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
                                        color = if (d == "CLR") ExpenseCoral else Color.White,
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
