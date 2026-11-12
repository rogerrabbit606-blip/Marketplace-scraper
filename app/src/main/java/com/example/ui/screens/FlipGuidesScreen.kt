package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MarketplaceEngine
import com.example.model.ResaleGuideCategory
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.GoldCash
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SkyComps
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard

@Composable
fun FlipGuidesScreen(
    freeOnlyFilter: Boolean?,
    onFilterChange: (Boolean?) -> Unit,
    onScoutGuide: (ResaleGuideCategory) -> Unit
) {
    val context = LocalContext.current
    val guides = when (freeOnlyFilter) {
        true -> MarketplaceEngine.RESALE_GUIDE_ITEMS.filter { it.defaultFreeSearch }
        false -> MarketplaceEngine.RESALE_GUIDE_ITEMS.filter { !it.defaultFreeSearch }
        null -> MarketplaceEngine.RESALE_GUIDE_ITEMS
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Hot Resale Categories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Proven free-pickup and cash-flip categories, what to inspect before buying, and one-tap marketplace search presets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = freeOnlyFilter == null,
                        onClick = { onFilterChange(null) },
                        label = { Text("All Niches") },
                        modifier = Modifier.testTag("guide_filter_all")
                    )
                    FilterChip(
                        selected = freeOnlyFilter == true,
                        onClick = { onFilterChange(true) },
                        label = { Text("$0 Free-Friendly") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldFree
                        ),
                        modifier = Modifier.testTag("guide_filter_free")
                    )
                    FilterChip(
                        selected = freeOnlyFilter == false,
                        onClick = { onFilterChange(false) },
                        label = { Text("Cash Flips") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = GoldCash
                        ),
                        modifier = Modifier.testTag("guide_filter_cash")
                    )
                }
            }

            items(guides, key = { it.id }) { guide ->
                ResaleGuideCard(
                    guide = guide,
                    onScoutClick = { onScoutGuide(guide) },
                    onCompsClick = {
                        val url = MarketplaceEngine.buildEbaySoldCompsUrl(guide.searchKeywords)
                        MarketplaceEngine.openExternalUrl(context, url)
                    }
                )
            }
        }
    }
}

@Composable
private fun ResaleGuideCard(
    guide: ResaleGuideCategory,
    onScoutClick: () -> Unit,
    onCompsClick: () -> Unit
) {
    val isFree = guide.defaultFreeSearch
    val badgeColor = if (isFree) EmeraldFree else GoldCash
    val badgeBg = if (isFree) EmeraldContainer else GoldContainer
    val buyLabel = if (isFree) "FREE ($0) TARGET" else "BUY <= $${guide.suggestedMaxCash}"

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
            .testTag("guide_card_${guide.id}")
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
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = buyLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = SkyComps,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = guide.flipSpeed,
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyComps
                    )
                }
            }

            Text(
                text = guide.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Typical Resale: ${guide.typicalResaleRange}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFamily,
                color = EmeraldFree
            )

            Text(
                text = "Look for: ${guide.whatToLookFor}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = "Inspection red flags",
                    tint = CoralRed,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Text(
                    text = "Red flags: ${guide.inspectionRedFlags}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onScoutClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldFree,
                        contentColor = Color(0xFF022C22)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("scout_guide_${guide.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load in Scout", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCompsClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("comps_guide_${guide.id}")
                ) {
                    Text("eBay Comps")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Check eBay Sold Comps",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
