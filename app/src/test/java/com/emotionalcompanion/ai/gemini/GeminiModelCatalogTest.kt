package com.emotionalcompanion.ai.gemini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelCatalogTest {

    @Test
    fun defaultModelHasConfirmedFreeTier() {
        val default = GeminiModelCatalog.liveModels.first { it.id == GeminiModelCatalog.defaultModelId }
        assertEquals(LiveModel.FreeTier.Confirmed, default.freeTier)
    }

    @Test
    fun freeOnlyHidesModelsWithoutConfirmedFreeTier() {
        val free = GeminiModelCatalog.available(freeOnly = true)
        assertTrue(free.isNotEmpty())
        assertTrue(free.all { it.freeTier == LiveModel.FreeTier.Confirmed })
        assertEquals(GeminiModelCatalog.liveModels.size, GeminiModelCatalog.available(freeOnly = false).size)
    }

    @Test
    fun resolveFallsBackWhenSavedModelIsNotAllowed() {
        assertEquals(GeminiModelCatalog.defaultModelId, GeminiModelCatalog.resolve("gemini-3.8-live", freeOnly = true))
        assertEquals("gemini-3.8-live", GeminiModelCatalog.resolve("gemini-3.8-live", freeOnly = false))
        assertEquals(GeminiModelCatalog.defaultModelId, GeminiModelCatalog.resolve(null, freeOnly = false))
        assertEquals(GeminiModelCatalog.defaultModelId, GeminiModelCatalog.resolve("made-up-model", freeOnly = false))
    }

    @Test
    fun modelIdsAreUnique() {
        val ids = GeminiModelCatalog.liveModels.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
