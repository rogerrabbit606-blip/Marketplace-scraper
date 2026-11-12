package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class DealMode(val wireValue: String, val label: String) {
    FREE_ONLY("FREE_ONLY", "Free ($0)"),
    CASH_BARGAIN("CASH_BARGAIN", "Cash Deals");

    companion object {
        fun fromWire(value: String): DealMode =
            entries.firstOrNull { it.wireValue == value } ?: FREE_ONLY
    }
}

enum class AcquisitionType(val wireValue: String, val label: String) {
    FREE("FREE", "Free Pickup ($0)"),
    CASH("CASH", "Cash Buy");

    companion object {
        fun fromWire(value: String): AcquisitionType =
            entries.firstOrNull { it.wireValue == value } ?: CASH
    }
}

enum class FlipStatus(val wireValue: String, val label: String) {
    SCOUTING("SCOUTING", "Scouting"),
    BOUGHT("BOUGHT", "Bought"),
    LISTED("LISTED", "Listed"),
    SOLD("SOLD", "Sold");

    companion object {
        fun fromWire(value: String): FlipStatus =
            entries.firstOrNull { it.wireValue == value } ?: SCOUTING
    }
}

enum class MarketplacePlatform(val label: String) {
    FACEBOOK("Facebook Marketplace"),
    CRAIGSLIST("Craigslist"),
    OFFERUP("OfferUp"),
    NEXTDOOR("Nextdoor"),
    EBAY_COMPS("eBay Sold Comps"),
    YARD_SALE("Yard Sale"),
    OTHER("Other")
}

data class CityMarket(
    val slug: String,
    val name: String,
    val regionLabel: String
)

@IgnoreExtraProperties
data class SavedScout(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val keywords: String = "",
    val category: String = "All",
    val dealMode: String = DealMode.FREE_ONLY.wireValue,
    val maxPrice: Long = 0L,
    val citySlug: String = "sfbay",
    val cityName: String = "SF Bay Area",
    val radiusMiles: Long = 25L,
    val targetResale: Long = 100L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedDealMode: DealMode get() = DealMode.fromWire(dealMode)
}

@IgnoreExtraProperties
data class FlipItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val sourcePlatform: String = MarketplacePlatform.FACEBOOK.label,
    val category: String = "Furniture",
    val acquisitionType: String = AcquisitionType.FREE.wireValue,
    val buyPriceCents: Long = 0L,
    val repairCostCents: Long = 0L,
    val estResaleCents: Long = 0L,
    val soldPriceCents: Long = 0L,
    val status: String = FlipStatus.SCOUTING.wireValue,
    val listingUrl: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: FlipStatus get() = FlipStatus.fromWire(status)
    val parsedAcquisition: AcquisitionType get() = AcquisitionType.fromWire(acquisitionType)

    val totalCostCents: Long get() = buyPriceCents + repairCostCents

    val projectedProfitCents: Long
        get() = if (parsedStatus == FlipStatus.SOLD) {
            soldPriceCents - totalCostCents
        } else {
            estResaleCents - totalCostCents
        }

    val roiPercent: Int
        get() = if (totalCostCents <= 0L) {
            if (projectedProfitCents > 0L) 999 else 0
        } else {
            ((projectedProfitCents.toDouble() / totalCostCents.toDouble()) * 100.0).toInt()
        }
}

data class LiveListing(
    val id: String,
    val title: String,
    val priceText: String,
    val parsedPriceDollars: Int?,
    val locationText: String,
    val publishedText: String,
    val url: String,
    val source: String
)

data class ResaleGuideCategory(
    val id: String,
    val name: String,
    val searchKeywords: String,
    val defaultFreeSearch: Boolean,
    val suggestedMaxCash: Int,
    val typicalResaleRange: String,
    val flipSpeed: String,
    val whatToLookFor: String,
    val inspectionRedFlags: String
)

data class ProfitCalcState(
    val isFreePickup: Boolean = true,
    val cashBuyDollars: Int = 0,
    val gasAndRepairDollars: Int = 10,
    val estResaleDollars: Int = 140,
    val feePresetName: String = "Local Cash (0%)",
    val platformFeePercent: Double = 0.0,
    val fixedFeeDollars: Double = 0.0
) {
    val effectiveBuyDollars: Int get() = if (isFreePickup) 0 else cashBuyDollars
    val platformFeeDollars: Double get() = (estResaleDollars * platformFeePercent / 100.0) + fixedFeeDollars
    val totalOutlayDollars: Double get() = effectiveBuyDollars + gasAndRepairDollars + platformFeeDollars
    val netProfitDollars: Double get() = estResaleDollars - totalOutlayDollars
    val roiPercent: Int
        get() {
            val cashIn = effectiveBuyDollars + gasAndRepairDollars
            return if (cashIn <= 0) {
                if (netProfitDollars > 0) 999 else 0
            } else {
                ((netProfitDollars / cashIn.toDouble()) * 100.0).toInt()
            }
        }
    val maxSafeCashOfferDollars: Int
        get() {
            val targetProfit = 50.0
            val maxOffer = estResaleDollars - gasAndRepairDollars - platformFeeDollars - targetProfit
            return maxOffer.toInt().coerceAtLeast(0)
        }
}
