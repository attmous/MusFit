package com.musfit.integrations.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerResultTest {
    @Test
    fun resultsPreserveAdapterValuesWithoutFoodUiState() {
        assertEquals("4006381333931", BarcodeScanResult("4006381333931").value)
        assertEquals("Protein 20 g", NutritionLabelScanResult("Protein 20 g").text)
    }
}
