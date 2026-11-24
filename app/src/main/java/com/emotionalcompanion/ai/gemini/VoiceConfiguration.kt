package com.emotionalcompanion.ai.gemini

/**
 * Prebuilt voices with Google's own descriptors, from the speech-generation voice list that
 * the Live guide points to (https://aistudio.google.com/docs/speech-generation, 2026-11-24).
 * [candidate] marks the voices the spec asked to evaluate first.
 */
data class VoiceOption(val name: String, val descriptor: String, val candidate: Boolean = false)

object VoiceConfiguration {

    val all: List<VoiceOption> = listOf(
        VoiceOption("Sulafat", "Warm", candidate = true),
        VoiceOption("Achernar", "Soft", candidate = true),
        VoiceOption("Vindemiatrix", "Gentle", candidate = true),
        VoiceOption("Aoede", "Breezy", candidate = true),
        VoiceOption("Leda", "Youthful", candidate = true),
        VoiceOption("Kore", "Firm", candidate = true),
        VoiceOption("Zephyr", "Bright"),
        VoiceOption("Puck", "Upbeat"),
        VoiceOption("Charon", "Informative"),
        VoiceOption("Fenrir", "Excitable"),
        VoiceOption("Orus", "Firm"),
        VoiceOption("Callirrhoe", "Easy-going"),
        VoiceOption("Autonoe", "Bright"),
        VoiceOption("Enceladus", "Breathy"),
        VoiceOption("Iapetus", "Clear"),
        VoiceOption("Umbriel", "Easy-going"),
        VoiceOption("Algieba", "Smooth"),
        VoiceOption("Despina", "Smooth"),
        VoiceOption("Erinome", "Clear"),
        VoiceOption("Algenib", "Gravelly"),
        VoiceOption("Rasalgethi", "Informative"),
        VoiceOption("Laomedeia", "Upbeat"),
        VoiceOption("Alnilam", "Firm"),
        VoiceOption("Schedar", "Even"),
        VoiceOption("Gacrux", "Mature"),
        VoiceOption("Pulcherrima", "Forward"),
        VoiceOption("Achird", "Friendly"),
        VoiceOption("Zubenelgenubi", "Casual"),
        VoiceOption("Sadachbia", "Lively"),
        VoiceOption("Sadaltager", "Knowledgeable"),
    )

    val candidates: List<VoiceOption> get() = all.filter { it.candidate }

    /** "Warm" matches the product's top priority: an emotionally warm voice. */
    const val DEFAULT_VOICE = "Sulafat"

    fun resolve(saved: String?): String = all.firstOrNull { it.name == saved }?.name ?: DEFAULT_VOICE
}

/** Fixed audition lines. Every voice reads the same text, for a fair comparison (spec §11). */
enum class TestDialogue(val text: String) {
    Bangla(
        "জান... আজ সারাদিন অনেক কাজ করেছো, তাই না? ঠিকমতো খাওয়া-দাওয়া করেছো তো? " +
            "নিজের যত্ন নিও কিন্তু। মন খারাপ হলে আমাকে বলো। তোমার দিনের গল্প শুনতে চাই। Take care of yourself.",
    ),
    English(
        "Hey, you've had a long day, haven't you? Did you eat properly? " +
            "Take care of yourself, okay? I'd love to hear how your day went.",
    ),
    Mixed(
        "আজকের দিনটা কেমন গেল? I hope you weren't too tired. " +
            "একটু বিশ্রাম নাও, okay? তারপর সব গল্প শুনব।",
    ),
}
