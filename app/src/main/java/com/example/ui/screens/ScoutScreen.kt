package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MarketplaceEngine
import com.example.model.CityMarket
import com.example.model.DealMode
import com.example.model.LiveListing
import com.example.model.SavedScout
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.GoldCash
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SkyComps
import com.example.ui.theme.SkyContainer
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.viewmodel.LiveFeedUiState
import com.example.viewmodel.ScoutFilterState
import com.example.viewmodel.UiState

@Composable
fun ScoutScreen(
    filter: ScoutFilterState,
    savedScoutsState: UiState<List<SavedScout>>,
    liveFeedState: LiveFeedUiState,
    userDisplayName: String,
    onKeywordsChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDealModeChange: (DealMode) -> Unit,
    onMaxCashChange: (Int) -> Unit,
    onSelectCity: (CityMarket) -> Unit,
    onCustomCityChange: (String) -> Unit,
    onSaveScout: (String) -> Unit,
    onApplySavedScout: (SavedScout) -> Unit,
    onDeleteScout: (String) -> Unit,
    onScanLiveFeed: () -> Unit,
    onTrackListing: (LiveListing) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showCityPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var scoutTitleInput by remember { mutableStateOf("") }
    var cashInput by remember(filter.maxCashDollars) { mutableStateOf(filter.maxCashDollars.toString()) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compact Header with Hero Art
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_scout_hero_1794516302559),
                        contentDescription = "FlipScout marketplace radar banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ObsidianBg.copy(alpha = 0.94f),
                                        ObsidianBg.copy(alpha = 0.72f),
                                        ObsidianBg.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "FlipScout",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Find $0 freebies & cash steals ready to flip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Scout: $userDisplayName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldFree,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(SlateCard.copy(alpha = 0.85f))
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Deal Finder Controls Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Deal Mode Segmented Switch: Free ($0) vs Cash Bargains
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianBg)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isFree = filter.dealMode == DealMode.FREE_ONLY
                            DealModeButton(
                                label = "$0 FREE ONLY",
                                selected = isFree,
                                activeColor = EmeraldFree,
                                onClick = { onDealModeChange(DealMode.FREE_ONLY) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_free_only")
                            )
                            DealModeButton(
                                label = "CASH BARGAINS",
                                selected = !isFree,
                                activeColor = GoldCash,
                                onClick = { onDealModeChange(DealMode.CASH_BARGAIN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_cash_bargains")
                            )
                        }

                        // Keywords Search Box
                        OutlinedTextField(
                            value = filter.keywords,
                            onValueChange = onKeywordsChange,
                            label = { Text("What are you hunting for?") },
                            placeholder = { Text("e.g. solid wood dresser, dewalt, espresso, grill") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search item keywords",
                                    tint = if (filter.dealMode == DealMode.FREE_ONLY) EmeraldFree else GoldCash
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldFree,
                                unfocusedBorderColor = SlateBorder
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scout_keywords_input")
                        )

                        // Cash Cap Chips when in Cash Mode
                        AnimatedVisibility(visible = filter.dealMode == DealMode.CASH_BARGAIN) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Max Cash Offer: $${filter.maxCashDollars}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GoldCash,
                                    fontFamily = JetBrainsMonoFamily
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(25, 50, 75, 120, 200, 350).forEach { cap ->
                                        FilterChip(
                                            selected = filter.maxCashDollars == cap,
                                            onClick = {
                                                cashInput = cap.toString()
                                                onMaxCashChange(cap)
                                            },
                                            label = {
                                                Text(
                                                    "$$cap",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldContainer,
                                                selectedLabelColor = GoldCash
                                            ),
                                            modifier = Modifier.testTag("cash_cap_$cap")
                                        )
                                    }
                                    OutlinedTextField(
                                        value = cashInput,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }.take(5)
                                            cashInput = digits
                                            digits.toIntOrNull()?.let { onMaxCashChange(it) }
                                        },
                                        label = { Text("Custom $") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .width(108.dp)
                                            .testTag("custom_cash_input")
                                    )
                                }
                            }
                        }

                        // Category Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(MarketplaceEngine.RESALE_CATEGORIES) { cat ->
                                FilterChip(
                                    selected = filter.category == cat,
                                    onClick = { onCategoryChange(cat) },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldContainer,
                                        selectedLabelColor = EmeraldFree
                                    ),
                                    modifier = Modifier.testTag("category_chip_${cat.lowercase().replace(" ", "_")}")
                                )
                            }
                        }

                        // City Selector Row + Save Scout Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showCityPicker = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("select_city_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Local market city",
                                    tint = SkyComps,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = filter.effectiveCityName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    scoutTitleInput = ""
                                    showSaveDialog = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("save_scout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BookmarkAdd,
                                    contentDescription = "Save scout alert",
                                    tint = GoldCash,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save")
                            }
                        }
                    }
                }
            }

            // One-Tap Live Marketplace Launchers
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Open Filtered Marketplace Deals",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MarketplaceLaunchCard(
                            name = "FB Marketplace",
                            badge = if (filter.dealMode == DealMode.FREE_ONLY) "$0 Newest" else "<=$${filter.effectiveMaxPrice} Local",
                            accent = SkyComps,
                            onClick = {
                                val url = MarketplaceEngine.buildFacebookMarketplaceUrl(
                                    filter.keywords,
                                    filter.dealMode,
                                    filter.effectiveMaxPrice
                                )
                                MarketplaceEngine.openExternalUrl(context, url)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("launch_facebook_button")
                        )
                        MarketplaceLaunchCard(
                            name = "Craigslist",
                            badge = if (filter.dealMode == DealMode.FREE_ONLY) "Free Stuff" else "By Owner",
                            accent = EmeraldFree,
                            onClick = {
                                val url = MarketplaceEngine.buildCraigslistUrl(
                                    filter.effectiveCitySlug,
                                    filter.keywords,
                                    filter.dealMode,
                                    filter.effectiveMaxPrice
                                )
                                MarketplaceEngine.openExternalUrl(context, url)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("launch_craigslist_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MarketplaceLaunchCard(
                            name = "OfferUp Local",
                            badge = if (filter.dealMode == DealMode.FREE_ONLY) "Free Pickup" else "Cash Deals",
                            accent = GoldCash,
                            onClick = {
                                val url = MarketplaceEngine.buildOfferUpUrl(
                                    filter.keywords,
                                    filter.dealMode,
                                    filter.effectiveMaxPrice
                                )
                                MarketplaceEngine.openExternalUrl(context, url)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("launch_offerup_button")
                        )
                        MarketplaceLaunchCard(
                            name = "eBay Sold Comps",
                            badge = "Verify Resale $",
                            accent = Color(0xFFA78BFA),
                            onClick = {
                                val url = MarketplaceEngine.buildEbaySoldCompsUrl(filter.keywords)
                                MarketplaceEngine.openExternalUrl(context, url)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("launch_ebay_comps_button")
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val url = MarketplaceEngine.buildNextdoorUrl(filter.keywords, filter.dealMode)
                            MarketplaceEngine.openExternalUrl(context, url)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_nextdoor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = "Open Nextdoor Finds",
                            tint = EmeraldFree,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Nextdoor For Sale & Free")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Saved Scouts Section (Firestore Synced)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your Saved Deal Scouts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    when (savedScoutsState) {
                        is UiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Syncing saved scouts...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is UiState.Error -> {
                            Text(
                                text = savedScoutsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is UiState.Success -> {
                            if (savedScoutsState.data.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No saved scouts yet. Configure a Free or Cash search above and tap Save.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(savedScoutsState.data, key = { it.id }) { scout ->
                                        SavedScoutChipCard(
                                            scout = scout,
                                            onApply = { onApplySavedScout(scout) },
                                            onDelete = { onDeleteScout(scout.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live In-App Local RSS Scanner (Craigslist)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Local Feed Scanner",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fetches real recent Craigslist ${if (filter.dealMode == DealMode.FREE_ONLY) "Free" else "Cash"} RSS posts in ${filter.effectiveCityName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onScanLiveFeed,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldFree,
                                    contentColor = Color(0xFF022C22)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("scan_live_feed_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Scan live feed",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan", fontWeight = FontWeight.Bold)
                            }
                        }

                        when (liveFeedState) {
                            is LiveFeedUiState.Idle -> {
                                Text(
                                    text = "Tap Scan to pull live posts right here, or use the marketplace buttons above to open Facebook, Craigslist, or OfferUp directly.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is LiveFeedUiState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text("Checking live local feed...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            is LiveFeedUiState.Unavailable -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = liveFeedState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GoldCash
                                    )
                                    OutlinedButton(
                                        onClick = { MarketplaceEngine.openExternalUrl(context, liveFeedState.browserUrl) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Open Live Feed in Browser")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            is LiveFeedUiState.Success -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "${liveFeedState.listings.size} live listings found",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldFree
                                    )
                                    liveFeedState.listings.forEach { listing ->
                                        LiveListingRow(
                                            listing = listing,
                                            onOpen = { MarketplaceEngine.openExternalUrl(context, listing.url) },
                                            onComps = {
                                                val compsUrl = MarketplaceEngine.buildEbaySoldCompsUrl(listing.title)
                                                MarketplaceEngine.openExternalUrl(context, compsUrl)
                                            },
                                            onTrack = { onTrackListing(listing) }
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

    if (showCityPicker) {
        CityPickerDialog(
            selectedSlug = filter.effectiveCitySlug,
            customSlug = filter.customCitySlug,
            onSelectCity = {
                onSelectCity(it)
                showCityPicker = false
            },
            onApplyCustomSlug = {
                onCustomCityChange(it)
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Deal Scout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Save this ${filter.dealMode.label} search for ${filter.effectiveCityName} to your cloud account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = scoutTitleInput,
                        onValueChange = { scoutTitleInput = it },
                        label = { Text("Scout Name (optional)") },
                        placeholder = { Text("e.g. Free Oak Dressers") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveScout(scoutTitleInput)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldFree,
                        contentColor = Color(0xFF022C22)
                    )
                ) {
                    Text("Save Scout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DealModeButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) activeColor else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF022C22) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MarketplaceLaunchCard(
    name: String,
    badge: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SlateElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(82.dp)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open $name",
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Surface(
                color = accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedScoutChipCard(
    scout: SavedScout,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val isFree = scout.parsedDealMode == DealMode.FREE_ONLY
    val accent = if (isFree) EmeraldFree else GoldCash
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .widthIn(min = 180.dp, max = 240.dp)
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onApply)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scout.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${if (isFree) "$0 Free" else "<=$${scout.maxPrice}"} • ${scout.cityName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete scout ${scout.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveListingRow(
    listing: LiveListing,
    onOpen: () -> Unit,
    onComps: () -> Unit,
    onTrack: () -> Unit
) {
    val isFree = listing.parsedPriceDollars == 0 || listing.priceText.contains("FREE", ignoreCase = true)
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (isFree) EmeraldContainer else GoldContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = listing.priceText,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (isFree) EmeraldFree else GoldCash,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${listing.locationText} • ${listing.publishedText}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open listing",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onComps,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Sold comps",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Comps", maxLines = 1)
                }
                Button(
                    onClick = onTrack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldFree,
                        contentColor = Color(0xFF022C22)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Track flip",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Track", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CityPickerDialog(
    selectedSlug: String,
    customSlug: String,
    onSelectCity: (CityMarket) -> Unit,
    onApplyCustomSlug: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customInput by remember { mutableStateOf(customSlug) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Local Market") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    label = { Text("Custom Craigslist Subdomain") },
                    placeholder = { Text("e.g. sacramento, raleigh, denver") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (customInput.isNotBlank()) {
                    Button(
                        onClick = { onApplyCustomSlug(customInput) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use '${customInput.trim()}'")
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(MarketplaceEngine.CITY_MARKETS, key = { it.slug }) { city ->
                        val isSelected = city.slug == selectedSlug && customSlug.isBlank()
                        Surface(
                            onClick = { onSelectCity(city) },
                            color = if (isSelected) EmeraldContainer else SlateElevated,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) EmeraldFree else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = city.regionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
