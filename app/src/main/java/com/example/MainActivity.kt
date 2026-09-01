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
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AureumVectorIcon
import com.example.ui.components.HapticStyle
import com.example.ui.components.hapticClickable
import com.example.ui.components.rememberAppHaptics
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

                    // First-Time Tutorial Walkthrough Modal Overlay
                    val showTutorial by viewModel.showFirstTimeTutorial.collectAsState()
                    if (showTutorial && !isLocked) {
                        FirstTimeTutorialDialog(
                            onDismiss = { viewModel.completeTutorial() }
                        )
                    }

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

    val haptics = rememberAppHaptics()
    val tabOrder = remember { listOf("HOME", "TRANSACTIONS", "ACCOUNTS", "STATS", "LOANS", "MORE") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            // High visibility floating docking navigation bar with glassmorphic finish & haptics
            NavigationBarCustomDock(
                activeTab = activeTab,
                onTabSelect = { tabId ->
                    // Automatically close transaction sheet if open when tapping nav bar
                    showTransactionCreator = false
                    viewModel.editingTransaction.value = null
                    viewModel.activeTab.value = tabId
                },
                onCenterAdd = {
                    haptics.perform(HapticStyle.MEDIUM)
                    showTransactionCreator = true
                }
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
                    val initialIdx = tabOrder.indexOf(initialState).takeIf { it >= 0 } ?: 0
                    val targetIdx = tabOrder.indexOf(targetState).takeIf { it >= 0 } ?: 0
                    val isForward = targetIdx >= initialIdx

                    val slideFraction = 0.22f
                    val enterAnim = if (isForward) {
                        slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { (it * slideFraction).toInt() }
                        )
                    } else {
                        slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { -(it * slideFraction).toInt() }
                        )
                    }

                    val exitAnim = if (isForward) {
                        slideOutHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            targetOffsetX = { -(it * slideFraction).toInt() }
                        )
                    } else {
                        slideOutHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            targetOffsetX = { (it * slideFraction).toInt() }
                        )
                    }

                    (enterAnim + fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.82f))) togetherWith
                    (exitAnim + fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) + scaleOut(targetScale = 1.03f, animationSpec = tween(180)))
                },
                label = "MainTabsGlassmorphicTransition"
            ) { targetTab ->
                when (targetTab) {
                    "HOME" -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onAddTransactionClick = { 
                                haptics.perform(HapticStyle.MEDIUM)
                                showTransactionCreator = true 
                            }
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
            
            // Transaction creator sheet launcher with spring glassmorphic pop-up
            AnimatedVisibility(
                visible = showTransactionCreator,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.85f)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
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
    val haptics = rememberAppHaptics()

    val dockBackground = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xF01E2430),
                Color(0xE0121620)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xF5FFFFFF),
                Color(0xEDF1F5F9)
            )
        )
    }

    val dockBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White,
                Color(0xFFE2E8F0)
            )
        )
    }

    val activeColor = if (isDark) NeonLime else CardBlack
    val inactiveColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Outer subtle drop glow for glassmorphism
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = if (isDark) 16.dp else 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = if (isDark) specs.neonGlowColor.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(dockBackground)
                .border(1.dp, dockBorderBrush, RoundedCornerShape(32.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 7 items: 3 left, 1 center (+), 3 right
                val tabs = listOf(
                    TabItem("HOME", "wallet_dock", "Home"),
                    TabItem("TRANSACTIONS", "receipt_long", "Ledger"),
                    TabItem("ACCOUNTS", "account_balance", "Vaults"),
                    TabItem("ADD", "add", "Add"),
                    TabItem("STATS", "pie_chart", "Analytics"),
                    TabItem("LOANS", "cards_dock", "Loans"),
                    TabItem("MORE", "hexagon_gear", "Settings")
                )

                tabs.forEach { tab ->
                    val isAdd = tab.id == "ADD"
                    val active = !isAdd && activeTab == tab.id
                    val targetColor = if (active) activeColor else inactiveColor
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else if (active) 1.06f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "tab_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = false, radius = 24.dp)
                            ) { 
                                if (isAdd) {
                                    haptics.perform(HapticStyle.MEDIUM)
                                    onCenterAdd()
                                } else {
                                    haptics.perform(HapticStyle.LIGHT)
                                    onTabSelect(tab.id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAdd) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                specs.activeContainer,
                                                if (isDark) NeonLimeLight else Color(0xFF1E2430)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
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
                                Box(contentAlignment = Alignment.Center) {
                                    if (active) {
                                        // Glass glowing pill behind active tab icon
                                        Box(
                                            modifier = Modifier
                                                .size(width = 36.dp, height = 30.dp)
                                                .clip(RoundedCornerShape(15.dp))
                                                .background(
                                                    if (isDark) specs.activeContainer.copy(alpha = 0.15f)
                                                    else specs.activeContainer.copy(alpha = 0.08f)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    specs.activeContainer.copy(alpha = 0.3f),
                                                    RoundedCornerShape(15.dp)
                                                )
                                        )
                                    }
                                    AureumVectorIcon(
                                        name = tab.icon,
                                        tint = targetColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                if (active) {
                                    Spacer(modifier = Modifier.height(2.dp))
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
}

data class TabItem(val id: String, val icon: String, val label: String)

data class TutorialStepData(
    val title: String,
    val badge: String,
    val icon: String,
    val subtitle: String,
    val accentColor: Color,
    val description: AnnotatedString
)

@Composable
fun FirstTimeTutorialDialog(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val specs = AureumThemeStyles.specs
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Color definitions for vibrant highlights
    val greenColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val amberColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val cyanColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val blueColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val purpleColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    val coralColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    val pinkColor = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
    val limeColor = if (isDark) Color(0xFFB0FF27) else Color(0xFF4D7C0F)

    val tutorialSteps = listOf(
        TutorialStepData(
            title = "Welcome to Aureum",
            badge = "QUICK TOUR • 1/5",
            icon = "wallet_dock",
            subtitle = "Smart Wealth & Debt Management",
            accentColor = greenColor,
            description = buildAnnotatedString {
                append("Aureum is your ")
                withStyle(SpanStyle(color = greenColor, fontWeight = FontWeight.Bold)) {
                    append("ultra-private")
                }
                append(", ")
                withStyle(SpanStyle(color = limeColor, fontWeight = FontWeight.Bold)) {
                    append("100% offline")
                }
                append(" financial ledger. Track your ")
                withStyle(SpanStyle(color = amberColor, fontWeight = FontWeight.Bold)) {
                    append("total net worth")
                }
                append(", manage custom vaults, log daily transactions, and keep soft loans organized with ")
                withStyle(SpanStyle(color = cyanColor, fontWeight = FontWeight.Bold)) {
                    append("zero cloud leaks")
                }
                append(".")
            }
        ),
        TutorialStepData(
            title = "Vault Cards & Balances",
            badge = "VAULTS • 2/5",
            icon = "credit_card",
            subtitle = "Cash, Cards & Bank Accounts",
            accentColor = blueColor,
            description = buildAnnotatedString {
                append("Swipe left and right on your home screen to switch between accounts:\n\n")
                withStyle(SpanStyle(color = greenColor, fontWeight = FontWeight.Bold)) {
                    append("• Cash Vault: ")
                }
                append("Physical cash on hand\n")
                withStyle(SpanStyle(color = coralColor, fontWeight = FontWeight.Bold)) {
                    append("• Credit Cards: ")
                }
                append("Credit limits & dues\n")
                withStyle(SpanStyle(color = blueColor, fontWeight = FontWeight.Bold)) {
                    append("• Bank Vaults: ")
                }
                append("Checking & savings\n\nTap on any card to view detailed ledger stats, or head to the ")
                withStyle(SpanStyle(color = purpleColor, fontWeight = FontWeight.Bold)) {
                    append("Vaults Tab (3rd)")
                }
                append(" to add new cards or customize colors.")
            }
        ),
        TutorialStepData(
            title = "7-Tab Navigation Dock",
            badge = "NAVIGATION • 3/5",
            icon = "drag_handle",
            subtitle = "Everything At Your Fingertips",
            accentColor = purpleColor,
            description = buildAnnotatedString {
                withStyle(SpanStyle(color = greenColor, fontWeight = FontWeight.Bold)) {
                    append("• Home: ")
                }
                append("Live overview & balance cards\n")
                withStyle(SpanStyle(color = blueColor, fontWeight = FontWeight.Bold)) {
                    append("• Ledger: ")
                }
                append("Full transaction history & search\n")
                withStyle(SpanStyle(color = purpleColor, fontWeight = FontWeight.Bold)) {
                    append("• Vaults (3rd): ")
                }
                append("Card editor & balance sheets\n")
                withStyle(SpanStyle(color = limeColor, fontWeight = FontWeight.Bold)) {
                    append("• (+) Center (4th): ")
                }
                append("Instant quick-add transaction\n")
                withStyle(SpanStyle(color = pinkColor, fontWeight = FontWeight.Bold)) {
                    append("• Analytics: ")
                }
                append("Category breakdown charts\n")
                withStyle(SpanStyle(color = amberColor, fontWeight = FontWeight.Bold)) {
                    append("• Loans: ")
                }
                append("Soft debts, lenders & EMI schedules\n")
                withStyle(SpanStyle(color = cyanColor, fontWeight = FontWeight.Bold)) {
                    append("• Settings: ")
                }
                append("PIN, Biometrics & Offline Backups")
            }
        ),
        TutorialStepData(
            title = "Instant (+) Transaction Logger",
            badge = "ADD TRANSACTIONS • 4/5",
            icon = "add",
            subtitle = "Center Quick-Add Button",
            accentColor = amberColor,
            description = buildAnnotatedString {
                append("Tap the glowing ")
                withStyle(SpanStyle(color = limeColor, fontWeight = FontWeight.Bold)) {
                    append("(+) center button")
                }
                append(" anytime to record ")
                withStyle(SpanStyle(color = coralColor, fontWeight = FontWeight.Bold)) {
                    append("Expense")
                }
                append(", ")
                withStyle(SpanStyle(color = greenColor, fontWeight = FontWeight.Bold)) {
                    append("Income")
                }
                append(", or ")
                withStyle(SpanStyle(color = blueColor, fontWeight = FontWeight.Bold)) {
                    append("Transfer")
                }
                append(". Attach ")
                withStyle(SpanStyle(color = purpleColor, fontWeight = FontWeight.Bold)) {
                    append("receipt photos")
                }
                append(", assign categories, or log soft debt repayments effortlessly.")
            }
        ),
        TutorialStepData(
            title = "Bank-Grade Privacy & Security",
            badge = "SECURITY • 5/5",
            icon = "fingerprint",
            subtitle = "100% Offline & Biometric Guard",
            accentColor = cyanColor,
            description = buildAnnotatedString {
                append("Your financial records stay ")
                withStyle(SpanStyle(color = greenColor, fontWeight = FontWeight.Bold)) {
                    append("strictly on your local device")
                }
                append(". Enable ")
                withStyle(SpanStyle(color = cyanColor, fontWeight = FontWeight.Bold)) {
                    append("Fingerprint / Face Unlock")
                }
                append(" or set a custom ")
                withStyle(SpanStyle(color = amberColor, fontWeight = FontWeight.Bold)) {
                    append("4-digit PIN Passcode")
                }
                append(" in Settings. Turn on ")
                withStyle(SpanStyle(color = purpleColor, fontWeight = FontWeight.Bold)) {
                    append("Anti-Screenshot Guard")
                }
                append(" for maximum peace of mind.")
            }
        )
    )

    val currentData = tutorialSteps[currentStep]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(enabled = false) {}, // Prevent outside touches
            contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E2430) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Badge & Icon with colorful accent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentData.accentColor.copy(alpha = 0.18f))
                            .border(1.dp, currentData.accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentData.badge,
                            color = currentData.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(currentData.accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        AureumVectorIcon(
                            name = currentData.icon,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Title and Subtitle with colorful accent
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = currentData.title,
                        color = specs.contentPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentData.subtitle,
                        color = currentData.accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Description Box with Rich Colorful Text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF141822) else Color(0xFFF8FAFC))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentData.description,
                        color = specs.contentSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Step Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(tutorialSteps.size) { idx ->
                        val isCurrent = idx == currentStep
                        val dotWidth by animateFloatAsState(
                            targetValue = if (isCurrent) 22f else 6f,
                            label = "tutorial_dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(dotWidth.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) currentData.accentColor else specs.inactiveContainer
                                )
                        )
                    }
                }

                // Action Buttons: Skip & Next / Finish
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val haptics = rememberAppHaptics()

                    // Skip button to fully skip tutorial
                    TextButton(
                        onClick = { 
                            haptics.perform(HapticStyle.LIGHT)
                            onDismiss() 
                        }
                    ) {
                        Text(
                            text = "Skip Tour",
                            color = specs.contentSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Next button or Get Started
                    Button(
                        onClick = {
                            if (currentStep < tutorialSteps.size - 1) {
                                haptics.perform(HapticStyle.LIGHT)
                                currentStep++
                            } else {
                                haptics.perform(HapticStyle.SUCCESS)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentData.accentColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (currentStep == tutorialSteps.size - 1) "Get Started" else "Next",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockscreenOverlay(
    viewModel: AureumViewModel,
    onBiometricClick: () -> Unit = {}
) {
    val pinVal by viewModel.pinInput.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val specs = AureumThemeStyles.specs
    val haptics = rememberAppHaptics()

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
                                        .hapticClickable(style = HapticStyle.MEDIUM) { 
                                            onBiometricClick() 
                                        },
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
                                        .hapticClickable(
                                            style = if (d == "CLR") HapticStyle.HEAVY else HapticStyle.LIGHT
                                        ) {
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
