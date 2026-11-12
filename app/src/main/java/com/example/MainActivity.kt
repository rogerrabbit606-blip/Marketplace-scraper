package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FlipRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FlipGuidesScreen
import com.example.ui.screens.PipelineScreen
import com.example.ui.screens.ProfitCalcScreen
import com.example.ui.screens.ScoutScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.FlipScoutTheme
import com.example.ui.theme.GoldCash
import com.example.viewmodel.FlipNavTab
import com.example.viewmodel.FlipScoutViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlipScoutTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener emits updated user */ })
    } else {
        key(user.uid) {
            FlipScoutMainApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "Scout"
            )
        }
    }
}

@Composable
fun FlipScoutMainApp(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: FlipScoutViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FlipScoutViewModel(FlipRepository(db), currentUserId)
            }
        }
    )

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val scoutFilter by viewModel.scoutFilter.collectAsStateWithLifecycle()
    val savedScoutsState by viewModel.savedScouts.collectAsStateWithLifecycle()
    val liveFeedState by viewModel.liveFeedState.collectAsStateWithLifecycle()
    val calcState by viewModel.calcState.collectAsStateWithLifecycle()
    val guideFilterFreeOnly by viewModel.guideFilterFreeOnly.collectAsStateWithLifecycle()
    val flipItemsState by viewModel.flipItems.collectAsStateWithLifecycle()
    val pipelineFilter by viewModel.pipelineStatusFilter.collectAsStateWithLifecycle()

    val activeFlipsCount = (flipItemsState as? UiState.Success)?.data?.size ?: 0

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = currentTab != FlipNavTab.SCOUT) {
        viewModel.setTab(FlipNavTab.SCOUT)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("main_bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == FlipNavTab.SCOUT,
                            onClick = { viewModel.setTab(FlipNavTab.SCOUT) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == FlipNavTab.SCOUT) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Scout"
                                )
                            },
                            label = { Text("Scout", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldFree,
                                indicatorColor = EmeraldContainer,
                                selectedTextColor = EmeraldFree
                            ),
                            modifier = Modifier.testTag("nav_tab_scout")
                        )

                        NavigationBarItem(
                            selected = currentTab == FlipNavTab.GUIDES,
                            onClick = { viewModel.setTab(FlipNavTab.GUIDES) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == FlipNavTab.GUIDES) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment,
                                    contentDescription = "Hot Flips"
                                )
                            },
                            label = { Text("Hot Flips", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GoldCash,
                                indicatorColor = EmeraldContainer,
                                selectedTextColor = GoldCash
                            ),
                            modifier = Modifier.testTag("nav_tab_guides")
                        )

                        NavigationBarItem(
                            selected = currentTab == FlipNavTab.CALCULATOR,
                            onClick = { viewModel.setTab(FlipNavTab.CALCULATOR) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == FlipNavTab.CALCULATOR) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                                    contentDescription = "Profit Calc"
                                )
                            },
                            label = { Text("Profit", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldFree,
                                indicatorColor = EmeraldContainer,
                                selectedTextColor = EmeraldFree
                            ),
                            modifier = Modifier.testTag("nav_tab_calculator")
                        )

                        NavigationBarItem(
                            selected = currentTab == FlipNavTab.PIPELINE,
                            onClick = { viewModel.setTab(FlipNavTab.PIPELINE) },
                            icon = {
                                if (activeFlipsCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = GoldCash) {
                                                Text("$activeFlipsCount", color = Color(0xFF271300))
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentTab == FlipNavTab.PIPELINE) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                                            contentDescription = "My Flips"
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (currentTab == FlipNavTab.PIPELINE) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                                        contentDescription = "My Flips"
                                    )
                                }
                            },
                            label = { Text("My Flips", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GoldCash,
                                indicatorColor = EmeraldContainer,
                                selectedTextColor = GoldCash
                            ),
                            modifier = Modifier.testTag("nav_tab_pipeline")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = currentTab == FlipNavTab.SCOUT,
                            onClick = { viewModel.setTab(FlipNavTab.SCOUT) },
                            icon = { Icon(Icons.Filled.Search, contentDescription = "Scout") },
                            label = { Text("Scout") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = EmeraldFree,
                                indicatorColor = EmeraldContainer
                            )
                        )
                        NavigationRailItem(
                            selected = currentTab == FlipNavTab.GUIDES,
                            onClick = { viewModel.setTab(FlipNavTab.GUIDES) },
                            icon = { Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Hot Flips") },
                            label = { Text("Hot Flips") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = GoldCash,
                                indicatorColor = EmeraldContainer
                            )
                        )
                        NavigationRailItem(
                            selected = currentTab == FlipNavTab.CALCULATOR,
                            onClick = { viewModel.setTab(FlipNavTab.CALCULATOR) },
                            icon = { Icon(Icons.Filled.Calculate, contentDescription = "Profit") },
                            label = { Text("Profit") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = EmeraldFree,
                                indicatorColor = EmeraldContainer
                            )
                        )
                        NavigationRailItem(
                            selected = currentTab == FlipNavTab.PIPELINE,
                            onClick = { viewModel.setTab(FlipNavTab.PIPELINE) },
                            icon = { Icon(Icons.Filled.Inventory2, contentDescription = "My Flips") },
                            label = { Text("My Flips") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = GoldCash,
                                indicatorColor = EmeraldContainer
                            )
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "flipscout_tab_transition"
                    ) { tab ->
                        when (tab) {
                            FlipNavTab.SCOUT -> {
                                ScoutScreen(
                                    filter = scoutFilter,
                                    savedScoutsState = savedScoutsState,
                                    liveFeedState = liveFeedState,
                                    userDisplayName = userDisplayName,
                                    onKeywordsChange = viewModel::updateKeywords,
                                    onCategoryChange = viewModel::updateCategory,
                                    onDealModeChange = viewModel::updateDealMode,
                                    onMaxCashChange = viewModel::updateMaxCash,
                                    onSelectCity = viewModel::selectCity,
                                    onCustomCityChange = viewModel::updateCustomCitySlug,
                                    onSaveScout = viewModel::saveCurrentScout,
                                    onApplySavedScout = viewModel::applySavedScout,
                                    onDeleteScout = viewModel::deleteScout,
                                    onScanLiveFeed = viewModel::scanLiveCraigslistFeed,
                                    onTrackListing = viewModel::trackLiveListing,
                                    onSignOut = {
                                        signOutUser(credentialManager, scope)
                                    }
                                )
                            }
                            FlipNavTab.GUIDES -> {
                                FlipGuidesScreen(
                                    freeOnlyFilter = guideFilterFreeOnly,
                                    onFilterChange = viewModel::setGuideFilter,
                                    onScoutGuide = viewModel::applyGuideToScout
                                )
                            }
                            FlipNavTab.CALCULATOR -> {
                                ProfitCalcScreen(
                                    calcState = calcState,
                                    onUpdateCalculator = viewModel::updateCalculator,
                                    onSaveToPipeline = viewModel::saveCalculatorDealToPipeline
                                )
                            }
                            FlipNavTab.PIPELINE -> {
                                PipelineScreen(
                                    flipsState = flipItemsState,
                                    selectedFilter = pipelineFilter,
                                    onFilterChange = viewModel::setPipelineFilter,
                                    onCreateFlip = viewModel::createFlip,
                                    onAdvanceStatus = viewModel::advanceFlipStatus,
                                    onDeleteFlip = viewModel::deleteFlip
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
