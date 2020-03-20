package edu.wpi.axon.db.data

import edu.wpi.axon.examplemodel.ExampleModel
import edu.wpi.axon.util.FilePath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

/**
 * The various sources a model could be loaded from.
 */
@Serializable
sealed class ModelSource {

    abstract val filename: String

    /**
     * From an example model.
     */
    @Serializable
    data class FromExample(val exampleModel: ExampleModel) : ModelSource() {
        override val filename: String = exampleModel.fileName
    }

    /**
     * From a FilePath.
     */
    @Serializable
    data class FromFile(val filePath: FilePath) : ModelSource() {
        override val filename: String = filePath.filename
    }

    /**
     * From the trained output of a Job.
     */
    @Serializable
    data class FromJob(val jobId: Int) : ModelSource() {
        override val filename: String
            get() = TODO("Not yet implemented")
    }

    fun serialize(): String = Json(
        JsonConfiguration.Stable
    ).stringify(serializer(), this)

    companion object {
        fun deserialize(data: String) = Json(
            JsonConfiguration.Stable
        ).parse(serializer(), data)
    }
}
