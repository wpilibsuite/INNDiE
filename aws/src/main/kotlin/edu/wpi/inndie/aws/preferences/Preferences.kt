package edu.wpi.inndie.aws.preferences

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import software.amazon.awssdk.services.ec2.model.InstanceType

/**
 * The user's preferences.
 *
 * @param defaultEC2NodeType The default EC2 node type to run training scripts in.
 * @param statusPollingDelay The time in milliseconds between training status polls.
 */
@Serializable
data class Preferences(
    var defaultEC2NodeType: InstanceType = InstanceType.T2_SMALL,
    var statusPollingDelay: Long = 5000
) {

    fun serialize(): String = Json(
        JsonConfiguration.Stable
    ).stringify(serializer(), this)

    companion object {
        fun deserialize(data: String) = Json(
            JsonConfiguration.Stable
        ).parse(serializer(), data)
    }
}
