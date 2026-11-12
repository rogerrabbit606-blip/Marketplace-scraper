package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MarketplaceEngine
import com.example.model.AcquisitionType
import com.example.model.FlipItem
import com.example.model.FlipStatus
import com.example.model.MarketplacePlatform
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.GoldCash
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SkyComps
import com.example.ui.theme.SkyContainer
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.viewmodel.UiState

@Composable
fun PipelineScreen(
    flipsState: UiState<List<FlipItem>>,
    selectedFilter: FlipStatus?,
    onFilterChange: (FlipStatus?) -> Unit,
    onCreateFlip: (FlipItem) -> Unit,
    onAdvanceStatus: (FlipItem, FlipStatus, Int?) -> Unit,
    onDeleteFlip: (String) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var itemPendingSold by remember { mutableStateOf<FlipItem?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (flipsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldFree)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading your flips...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = flipsState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CoralRed
                    )
                }
            }
            is UiState.Success -> {
                val allFlips = flipsState.data
                val filteredFlips = if (selectedFilter == null) {
                    allFlips
                } else {
                    allFlips.filter { it.parsedStatus == selectedFilter }
                }

                val realizedProfitDollars = allFlips
                    .filter { it.parsedStatus == FlipStatus.SOLD }
                    .sumOf { it.projectedProfitCents } / 100L
                val totalInvestedDollars = allFlips.sumOf { it.totalCostCents } / 100L
                val freeFindsCount = allFlips.count { it.parsedAcquisition == AcquisitionType.FREE }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 780.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "My Flip Pipeline",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Track free pickups & cash buys from scouting to profit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Summary Stats Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PipelineStatTile(
                                label = "Sold Profit",
                                value = "$${realizedProfitDollars}",
                                accent = EmeraldFree,
                                modifier = Modifier.weight(1f)
                            )
                            PipelineStatTile(
                                label = "Cash Outlay",
                                value = "$${totalInvestedDollars}",
                                accent = GoldCash,
                                modifier = Modifier.weight(1f)
                            )
                            PipelineStatTile(
                                label = "Free Finds",
                                value = "$freeFindsCount",
                                accent = SkyComps,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Status Filter Chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { onFilterChange(null) },
                                label = { Text("All (${allFlips.size})") },
                                modifier = Modifier.testTag("pipeline_filter_all")
                            )
                            FlipStatus.entries.forEach { st ->
                                val count = allFlips.count { it.parsedStatus == st }
                                FilterChip(
                                    selected = selectedFilter == st,
                                    onClick = { onFilterChange(st) },
                                    label = { Text("${st.label} ($count)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldContainer,
                                        selectedLabelColor = EmeraldFree
                                    ),
                                    modifier = Modifier.testTag("pipeline_filter_${st.wireValue.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filteredFlips.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint = EmeraldFree,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No flips in this stage yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap + Add Flip below or track a listing directly from the Scout tab.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredFlips, key = { it.id }) { item ->
                            FlipItemCard(
                                item = item,
                                onOpenUrl = {
                                    if (item.listingUrl.isNotBlank()) {
                                        MarketplaceEngine.openExternalUrl(context, item.listingUrl)
                                    }
                                },
                                onCheckComps = {
                                    val compsUrl = MarketplaceEngine.buildEbaySoldCompsUrl(item.title)
                                    MarketplaceEngine.openExternalUrl(context, compsUrl)
                                },
                                onAdvance = { nextStatus ->
                                    if (nextStatus == FlipStatus.SOLD) {
                                        itemPendingSold = item
                                    } else {
                                        onAdvanceStatus(item, nextStatus, null)
                                    }
                                },
                                onDelete = { onDeleteFlip(item.id) }
                            )
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldFree,
            contentColor = Color(0xFF022C22),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add Flip", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_flip_fab")
        )
    }

    if (showAddDialog) {
        AddFlipDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                onCreateFlip(newItem)
                showAddDialog = false
            }
        )
    }

    itemPendingSold?.let { item ->
        MarkSoldDialog(
            item = item,
            onDismiss = { itemPendingSold = null },
            onConfirmSold = { soldDollars ->
                onAdvanceStatus(item, FlipStatus.SOLD, soldDollars)
                itemPendingSold = null
            }
        )
    }
}

@Composable
private fun PipelineStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FlipItemCard(
    item: FlipItem,
    onOpenUrl: () -> Unit,
    onCheckComps: () -> Unit,
    onAdvance: (FlipStatus) -> Unit,
    onDelete: () -> Unit
) {
    val isFree = item.parsedAcquisition == AcquisitionType.FREE
    val status = item.parsedStatus
    val profitDollars = item.projectedProfitCents / 100L
    val buyDollars = item.buyPriceCents / 100L
    val repairDollars = item.repairCostCents / 100L
    val targetOrSoldDollars = if (status == FlipStatus.SOLD) {
        item.soldPriceCents / 100L
    } else {
        item.estResaleCents / 100L
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
            .testTag("flip_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (isFree) EmeraldContainer else GoldContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isFree) "$0 FREE" else "$$buyDollars CASH",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isFree) EmeraldFree else GoldCash,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = SkyContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = status.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkyComps,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete ${item.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${item.sourcePlatform} • ${item.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (status == FlipStatus.SOLD) "Sold: $$targetOrSoldDollars" else "Est Resale: $$targetOrSoldDollars",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (repairDollars > 0L) {
                    Text(
                        text = "Prep/Gas: $$repairDollars",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${if (profitDollars >= 0) "+" else "-"}$$${kotlin.math.abs(profitDollars)} Profit",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (profitDollars >= 0) EmeraldFree else CoralRed
                )
            }

            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.listingUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = onOpenUrl,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open listing", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Listing")
                    }
                }
                OutlinedButton(
                    onClick = onCheckComps,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "eBay Sold Comps", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Comps")
                }

                Spacer(modifier = Modifier.weight(1f))

                when (status) {
                    FlipStatus.SCOUTING -> {
                        Button(
                            onClick = { onAdvance(FlipStatus.BOUGHT) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldCash, contentColor = Color(0xFF271300)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Mark Bought", fontWeight = FontWeight.Bold)
                        }
                    }
                    FlipStatus.BOUGHT -> {
                        Button(
                            onClick = { onAdvance(FlipStatus.LISTED) },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyComps, contentColor = Color(0xFF082F49)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Mark Listed", fontWeight = FontWeight.Bold)
                        }
                    }
                    FlipStatus.LISTED -> {
                        Button(
                            onClick = { onAdvance(FlipStatus.SOLD) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldFree, contentColor = Color(0xFF022C22)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Sold", fontWeight = FontWeight.Bold)
                        }
                    }
                    FlipStatus.SOLD -> {
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldFree, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sold", style = MaterialTheme.typography.labelLarge, color = EmeraldFree)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFlipDialog(
    onDismiss: () -> Unit,
    onSave: (FlipItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var isFree by remember { mutableStateOf(true) }
    var buyDollars by remember { mutableStateOf("40") }
    var repairDollars by remember { mutableStateOf("0") }
    var estResaleDollars by remember { mutableStateOf("140") }
    var platform by remember { mutableStateOf(MarketplacePlatform.FACEBOOK.label) }
    var category by remember { mutableStateOf("Furniture") }
    var listingUrl by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Marketplace Find") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("e.g. Solid Oak Dresser") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_flip_title_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Free Pickup ($0)", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = isFree,
                        onCheckedChange = { isFree = it }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isFree) {
                        OutlinedTextField(
                            value = buyDollars,
                            onValueChange = { buyDollars = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("Cash Buy $") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = repairDollars,
                        onValueChange = { repairDollars = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Gas/Fix $") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = estResaleDollars,
                        onValueChange = { estResaleDollars = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Resale $") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        MarketplacePlatform.FACEBOOK.label,
                        MarketplacePlatform.CRAIGSLIST.label,
                        MarketplacePlatform.OFFERUP.label,
                        MarketplacePlatform.NEXTDOOR.label,
                        MarketplacePlatform.YARD_SALE.label
                    ).forEach { p ->
                        FilterChip(
                            selected = platform == p,
                            onClick = { platform = p },
                            label = { Text(p.replace(" Marketplace", "")) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MarketplaceEngine.RESALE_CATEGORIES.drop(1).forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) }
                        )
                    }
                }

                OutlinedTextField(
                    value = listingUrl,
                    onValueChange = { listingUrl = it },
                    label = { Text("Listing URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim().ifEmpty { "Marketplace Find" }
                    val buyCents = if (isFree) 0L else ((buyDollars.toLongOrNull() ?: 0L) * 100L)
                    val repairCents = (repairDollars.toLongOrNull() ?: 0L) * 100L
                    val resaleCents = (estResaleDollars.toLongOrNull() ?: 100L) * 100L
                    onSave(
                        FlipItem(
                            title = cleanTitle,
                            sourcePlatform = platform,
                            category = category,
                            acquisitionType = if (isFree) AcquisitionType.FREE.wireValue else AcquisitionType.CASH.wireValue,
                            buyPriceCents = buyCents,
                            repairCostCents = repairCents,
                            estResaleCents = resaleCents,
                            status = FlipStatus.SCOUTING.wireValue,
                            listingUrl = listingUrl.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldFree, contentColor = Color(0xFF022C22))
            ) {
                Text("Save Flip", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MarkSoldDialog(
    item: FlipItem,
    onDismiss: () -> Unit,
    onConfirmSold: (Int) -> Unit
) {
    var soldPriceInput by remember {
        mutableStateOf((item.estResaleCents / 100L).coerceAtLeast(1L).toString())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalize Sale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the final cash/payout you received for '${item.title}'. Once marked Sold, the record locks.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = soldPriceInput,
                    onValueChange = { soldPriceInput = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Actual Sold Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dollars = soldPriceInput.toIntOrNull() ?: (item.estResaleCents / 100L).toInt()
                    onConfirmSold(dollars)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldFree, contentColor = Color(0xFF022C22))
            ) {
                Text("Confirm Sold", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
