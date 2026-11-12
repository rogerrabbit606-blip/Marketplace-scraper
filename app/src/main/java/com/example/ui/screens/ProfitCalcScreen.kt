package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.MarketplaceEngine
import com.example.model.MarketplacePlatform
import com.example.model.ProfitCalcState
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.GoldCash
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SkyComps
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import kotlin.math.roundToInt

private data class FeePreset(
    val name: String,
    val percent: Double,
    val fixedDollars: Double
)

private val FEE_PRESETS = listOf(
    FeePreset("Local Cash (0%)", 0.0, 0.0),
    FeePreset("eBay (13.25%)", 13.25, 0.30),
    FeePreset("Mercari (10%)", 10.0, 0.0),
    FeePreset("FB Shipped (10%)", 10.0, 0.0)
)

@Composable
fun ProfitCalcScreen(
    calcState: ProfitCalcState,
    onUpdateCalculator: (
        isFreePickup: Boolean?,
        cashBuyDollars: Int?,
        gasAndRepairDollars: Int?,
        estResaleDollars: Int?,
        feePresetName: String?,
        platformFeePercent: Double?,
        fixedFeeDollars: Double?
    ) -> Unit,
    onSaveToPipeline: (title: String, platform: String, category: String, notes: String) -> Unit
) {
    var showLogDialog by remember { mutableStateOf(false) }

    val profit = calcState.netProfitDollars
    val (verdictLabel, verdictColor, verdictBg) = when {
        profit >= 75.0 && (calcState.isFreePickup || calcState.roiPercent >= 120) ->
            Triple("HOME RUN FLIP", EmeraldFree, EmeraldContainer)
        profit >= 35.0 ->
            Triple("SOLID CASH FLIP", GoldCash, GoldContainer)
        profit > 0.0 ->
            Triple("THIN MARGIN — NEGOTIATE", GoldCash, GoldContainer)
        else ->
            Triple("PASS / NEGOTIATE DOWN", CoralRed, CoralContainer)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Flip Profit Calculator",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Test if a free pickup or cash offer leaves enough margin after gas, parts, and selling fees.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Verdict & Net Profit Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, verdictColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = verdictBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = verdictLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = verdictColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Text(
                                text = if (calcState.roiPercent >= 999) "∞ ROI" else "${calcState.roiPercent}% ROI",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Estimated Net Profit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${if (profit >= 0) "+" else "-"}$${kotlin.math.abs(profit.roundToInt())}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = verdictColor,
                                    modifier = Modifier.testTag("calc_net_profit_value")
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Max Safe Cash Offer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${calcState.maxSafeCashOfferDollars}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = SkyComps
                                )
                                Text(
                                    text = "(keeps $50+ profit)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatCol("Buy Cost", "$${calcState.effectiveBuyDollars}")
                            StatCol("Gas / Fix", "$${calcState.gasAndRepairDollars}")
                            StatCol("Fees", "$${calcState.platformFeeDollars.roundToInt()}")
                            StatCol("Resale", "$${calcState.estResaleDollars}")
                        }
                    }
                }
            }

            // Inputs Card
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Free Curb / $0 Pickup",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Toggle off to enter your cash offer price",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = calcState.isFreePickup,
                                onCheckedChange = { free ->
                                    onUpdateCalculator(free, null, null, null, null, null, null)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF022C22),
                                    checkedTrackColor = EmeraldFree
                                ),
                                modifier = Modifier.testTag("calc_free_switch")
                            )
                        }

                        if (!calcState.isFreePickup) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cash Buy Price", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = "$${calcState.cashBuyDollars}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = GoldCash
                                    )
                                }
                                Slider(
                                    value = calcState.cashBuyDollars.toFloat().coerceIn(0f, 500f),
                                    onValueChange = {
                                        onUpdateCalculator(null, it.roundToInt(), null, null, null, null, null)
                                    },
                                    valueRange = 0f..500f,
                                    colors = SliderDefaults.colors(thumbColor = GoldCash, activeTrackColor = GoldCash),
                                    modifier = Modifier.testTag("calc_cash_slider")
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(15, 30, 50, 80, 120, 200).forEach { price ->
                                        FilterChip(
                                            selected = calcState.cashBuyDollars == price,
                                            onClick = {
                                                onUpdateCalculator(null, price, null, null, null, null, null)
                                            },
                                            label = { Text("$$price", fontFamily = JetBrainsMonoFamily) }
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Expected Resale Price", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "$${calcState.estResaleDollars}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = EmeraldFree
                                )
                            }
                            Slider(
                                value = calcState.estResaleDollars.toFloat().coerceIn(10f, 800f),
                                onValueChange = {
                                    onUpdateCalculator(null, null, null, it.roundToInt(), null, null, null)
                                },
                                valueRange = 10f..800f,
                                colors = SliderDefaults.colors(thumbColor = EmeraldFree, activeTrackColor = EmeraldFree),
                                modifier = Modifier.testTag("calc_resale_slider")
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gas, Cleaning & Repair Cost", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "$${calcState.gasAndRepairDollars}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = SkyComps
                                )
                            }
                            Slider(
                                value = calcState.gasAndRepairDollars.toFloat().coerceIn(0f, 150f),
                                onValueChange = {
                                    onUpdateCalculator(null, null, it.roundToInt(), null, null, null, null)
                                },
                                valueRange = 0f..150f,
                                colors = SliderDefaults.colors(thumbColor = SkyComps, activeTrackColor = SkyComps)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Where will you resell it?", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FEE_PRESETS.forEach { preset ->
                                    FilterChip(
                                        selected = calcState.feePresetName == preset.name,
                                        onClick = {
                                            onUpdateCalculator(
                                                null, null, null, null,
                                                preset.name, preset.percent, preset.fixedDollars
                                            )
                                        },
                                        label = { Text(preset.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldContainer,
                                            selectedLabelColor = EmeraldFree
                                        )
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showLogDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldFree,
                                contentColor = Color(0xFF022C22)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("calc_save_to_pipeline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddTask,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log Deal to My Flips",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var selectedPlatform by remember { mutableStateOf(MarketplacePlatform.FACEBOOK.label) }
        var selectedCategory by remember { mutableStateOf("Furniture") }

        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("Save Evaluated Deal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Item Title") },
                        placeholder = { Text("e.g. Oak Dresser or DeWalt Kit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            MarketplacePlatform.FACEBOOK.label,
                            MarketplacePlatform.CRAIGSLIST.label,
                            MarketplacePlatform.OFFERUP.label,
                            MarketplacePlatform.NEXTDOOR.label
                        ).forEach { plat ->
                            FilterChip(
                                selected = selectedPlatform == plat,
                                onClick = { selectedPlatform = plat },
                                label = { Text(plat.replace(" Marketplace", "")) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MarketplaceEngine.RESALE_CATEGORIES.drop(1).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("Pickup neighborhood, condition, etc.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveToPipeline(title, selectedPlatform, selectedCategory, notes)
                        showLogDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldFree,
                        contentColor = Color(0xFF022C22)
                    )
                ) {
                    Text("Save Deal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
