package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkField(
    var studyId: Int,
    var name: String,
    var type: String,
    var pii: Boolean,
    var required: Boolean,
    var integerOnly: Boolean,
    var date: Boolean,
    var time: Boolean,
    var option1: String,
    var option2: String,
    var option3: String,
    var option4: String )
{
}