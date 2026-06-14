package edu.gtri.gpssample.managers

import kotlinx.serialization.Serializable

/**
 * ============================================================================
 * Models
 * ============================================================================
 */

@Serializable
data class Request(
    val command: Command,
    val imageUuid: String? = null
)

@Serializable
enum class Command {
    GET_CONFIG,
    GET_ENUMERATION_ITEMS,
    GET_IMAGE,
    DONE,
    ACK_CONFIG,
    ACK_ENUMERATION_ITEMS,
    ACK_IMAGE
}
/**
 * ============================================================================
 * State
 * ============================================================================
 */

sealed interface NearbySessionState
{
    object Idle : NearbySessionState

    data class Advertising(
        val sessionId: String
    ) : NearbySessionState

    object Connecting : NearbySessionState
    object Connected : NearbySessionState
    object Done : NearbySessionState
    object SendingConfig : NearbySessionState
    object SendingEnumerationItems : NearbySessionState
    object SendingImage : NearbySessionState
    object ReceivingConfig : NearbySessionState
    object ReceivingEnumerationItems : NearbySessionState
    object ReceivingImages : NearbySessionState
    object Closed : NearbySessionState

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : NearbySessionState
}

class NearbySessionCore
{
    companion object { const val SERVICE_ID = "com.example.gpssample.transfer" }
}