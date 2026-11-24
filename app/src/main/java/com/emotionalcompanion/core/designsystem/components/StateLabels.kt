package com.emotionalcompanion.core.designsystem.components

import androidx.annotation.StringRes
import com.emotionalcompanion.R
import com.emotionalcompanion.domain.conversation.ConnectionStatus
import com.emotionalcompanion.domain.conversation.ConversationState

@StringRes
fun ConversationState.labelRes(): Int = when (this) {
    ConversationState.Idle -> R.string.state_idle
    ConversationState.Connecting -> R.string.state_connecting
    ConversationState.Listening -> R.string.state_listening
    ConversationState.Thinking -> R.string.state_thinking
    ConversationState.Speaking -> R.string.state_speaking
    ConversationState.Interrupted -> R.string.state_interrupted
    ConversationState.Reconnecting -> R.string.state_reconnecting
    ConversationState.Offline -> R.string.state_offline
    ConversationState.Error -> R.string.state_error
}

@StringRes
fun ConnectionStatus.labelRes(): Int = when (this) {
    ConnectionStatus.NotConfigured -> R.string.connection_not_configured
    ConnectionStatus.Connecting -> R.string.connection_connecting
    ConnectionStatus.Connected -> R.string.connection_connected
    ConnectionStatus.Reconnecting -> R.string.connection_reconnecting
    ConnectionStatus.Offline -> R.string.connection_offline
    ConnectionStatus.Error -> R.string.connection_error
}
