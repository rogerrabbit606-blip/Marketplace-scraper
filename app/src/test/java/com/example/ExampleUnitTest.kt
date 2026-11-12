package com.example

import com.example.model.AcquisitionType
import com.example.model.FlipItem
import com.example.model.FlipStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun flipItem_freeAcquisition_calculatesFullProfit() {
        val item = FlipItem(
            title = "Free Solid Walnut Table",
            acquisitionType = AcquisitionType.FREE.wireValue,
            buyPriceCents = 0L,
            repairCostCents = 1500L,
            estResaleCents = 18000L,
            status = FlipStatus.SCOUTING.wireValue
        )
        assertEquals(1500L, item.totalCostCents)
        assertEquals(16500L, item.projectedProfitCents)
    }

    @Test
    fun flipItem_soldStatus_usesSoldPriceForProfit() {
        val item = FlipItem(
            title = "DeWalt Miter Saw",
            acquisitionType = AcquisitionType.CASH.wireValue,
            buyPriceCents = 8000L,
            repairCostCents = 1000L,
            estResaleCents = 20000L,
            soldPriceCents = 22500L,
            status = FlipStatus.SOLD.wireValue
        )
        assertEquals(9000L, item.totalCostCents)
        assertEquals(13500L, item.projectedProfitCents)
        assertEquals(150, item.roiPercent)
    }
}
