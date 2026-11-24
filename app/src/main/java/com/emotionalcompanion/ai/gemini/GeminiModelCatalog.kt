package com.emotionalcompanion.ai.gemini

/**
 * Live (audio-to-audio) models, taken from official pages on 2026-11-24:
 * - IDs and status: https://ai.google.dev/gemini-api/docs/models
 * - Free tier:      https://ai.google.dev/gemini-api/docs/pricing
 * Update this list only from those pages, and record the date in docs/API_CONTRACT.md.
 */
data class LiveModel(
    val id: String,
    val status: Status,
    val freeTier: FreeTier,
) {
    enum class Status { Stable, Preview, Legacy }

    enum class FreeTier {
        /** The pricing page lists free input and output. */
        Confirmed,
        /** The model isn't on the pricing page; treat as possibly paid. */
        NotListed,
    }
}

object GeminiModelCatalog {

    val liveModels: List<LiveModel> = listOf(
        LiveModel("gemini-3.1-flash-live-preview", LiveModel.Status.Legacy, LiveModel.FreeTier.Confirmed),
        LiveModel("gemini-2.5-flash-native-audio-preview-12-2025", LiveModel.Status.Preview, LiveModel.FreeTier.Confirmed),
        LiveModel("gemini-3.8-live", LiveModel.Status.Stable, LiveModel.FreeTier.NotListed),
        LiveModel("gemini-3.8-live-extended-thinking", LiveModel.Status.Stable, LiveModel.FreeTier.NotListed),
    )

    /** Default model: the first one with a confirmed free tier. */
    val defaultModelId: String = liveModels.first { it.freeTier == LiveModel.FreeTier.Confirmed }.id

    /** In free-only mode, models without a confirmed free tier are hidden (spec §14). */
    fun available(freeOnly: Boolean): List<LiveModel> =
        if (freeOnly) liveModels.filter { it.freeTier == LiveModel.FreeTier.Confirmed } else liveModels

    /** Keeps a saved choice only if it is still allowed; otherwise falls back to the default. */
    fun resolve(savedId: String?, freeOnly: Boolean): String {
        val allowed = available(freeOnly)
        return allowed.firstOrNull { it.id == savedId }?.id ?: defaultModelId
    }
}
