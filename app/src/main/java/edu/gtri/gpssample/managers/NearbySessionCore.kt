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
    GET_ENUMERATION_AREAS,
    GET_IMAGE,
    DONE
}
/**
 * ============================================================================
 * State
 * ============================================================================
 */

sealed interface NearbySessionState
{
    object Idle : NearbySessionState

    data class Advertising( val sessionId: String) : NearbySessionState

    object Connecting : NearbySessionState
    object Connected : NearbySessionState
    object Done : NearbySessionState
    object SendingConfig : NearbySessionState
    data class SendingEnumerationAreas( val message: String ) : NearbySessionState
    object SendingImage : NearbySessionState
    object ReceivingConfig : NearbySessionState
    object ReceivingEnumerationAreas : NearbySessionState
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