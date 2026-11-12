package com.example.data

import com.example.model.DealMode
import com.example.model.ProfitCalcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceEngineTest {

    @Test
    fun buildFacebookMarketplaceUrl_freeMode_setsMaxPriceZero() {
        val url = MarketplaceEngine.buildFacebookMarketplaceUrl(
            keywords = "oak dresser",
            dealMode = DealMode.FREE_ONLY,
            maxPriceDollars = 100
        )
        assertTrue(url.contains("maxPrice=0"))
        assertTrue(url.contains("minPrice=0"))
        assertTrue(url.contains("query=oak+dresser"))
        assertTrue(url.contains("deliveryMethod=local_pick_up"))
    }

    @Test
    fun buildCraigslistUrl_cashMode_targetsOwnerWithMaxPrice() {
        val url = MarketplaceEngine.buildCraigslistUrl(
            citySlug = "austin",
            keywords = "dewalt drill",
            dealMode = DealMode.CASH_BARGAIN,
            maxPriceDollars = 85
        )
        assertTrue(url.startsWith("https://austin.craigslist.org/search/sss?"))
        assertTrue(url.contains("max_price=85"))
        assertTrue(url.contains("purveyor=owner"))
    }

    @Test
    fun buildCraigslistUrl_freeMode_targetsZipSection() {
        val url = MarketplaceEngine.buildCraigslistUrl(
            citySlug = "sfbay",
            keywords = "patio",
            dealMode = DealMode.FREE_ONLY,
            maxPriceDollars = 50
        )
        assertTrue(url.startsWith("https://sfbay.craigslist.org/search/zip?"))
    }

    @Test
    fun buildEbaySoldCompsUrl_includesSoldAndCompletedFlags() {
        val url = MarketplaceEngine.buildEbaySoldCompsUrl("kitchenaid mixer")
        assertTrue(url.contains("LH_Sold=1"))
        assertTrue(url.contains("LH_Complete=1"))
        assertTrue(url.contains("kitchenaid+mixer"))
    }

    @Test
    fun profitCalcState_calculatesCashProfitAndMaxSafeOfferCorrectly() {
        val state = ProfitCalcState(
            isFreePickup = false,
            cashBuyDollars = 50,
            gasAndRepairDollars = 10,
            estResaleDollars = 200,
            feePresetName = "Local Cash (0%)",
            platformFeePercent = 0.0,
            fixedFeeDollars = 0.0
        )
        assertEquals(140.0, state.netProfitDollars, 0.01)
        assertEquals(233, state.roiPercent)
        // 200 resale - 10 gas - 0 fees - 50 target profit = 140 max safe cash offer
        assertEquals(140, state.maxSafeCashOfferDollars)
    }
}
