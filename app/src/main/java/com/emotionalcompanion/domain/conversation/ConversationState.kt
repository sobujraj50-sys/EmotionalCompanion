package com.emotionalcompanion.domain.conversation

/** States of a voice conversation. The voice orb renders exactly one of these at a time (spec §8). */
enum class ConversationState {
    Idle,
    Connecting,
    Listening,
    Thinking,
    Speaking,
    Interrupted,
    Reconnecting,
    Offline,
    Error,
}

/** Link to Gemini. NotConfigured holds until an API key is saved (Milestone C). */
enum class ConnectionStatus {
    NotConfigured,
    Connecting,
    Connected,
    Reconnecting,
    Offline,
    Error,
}
