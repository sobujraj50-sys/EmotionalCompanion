package com.emotionalcompanion.ai.gemini

/**
 * Prebuilt voice names. The Live guide points to the speech-generation voice list
 * (https://aistudio.google.com/docs/speech-generation, checked 2026-11-24), which includes
 * these spec candidates with Google's own descriptors. Availability per Live model is
 * re-checked in Milestone E (Voice Studio), where the user picks the voice.
 */
data class VoiceOption(val name: String, val descriptor: String)

object VoiceConfiguration {
    val candidates: List<VoiceOption> = listOf(
        VoiceOption("Sulafat", "Warm"),
        VoiceOption("Achernar", "Soft"),
        VoiceOption("Vindemiatrix", "Gentle"),
        VoiceOption("Aoede", "Breezy"),
        VoiceOption("Leda", "Youthful"),
        VoiceOption("Kore", "Firm"),
    )

    /** "Warm" matches the product's top priority: an emotionally warm voice. */
    const val DEFAULT_VOICE = "Sulafat"
}
