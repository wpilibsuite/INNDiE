package edu.wpi.axon.dbdata

import edu.wpi.axon.util.ObjectSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

/**
 * The states a training script can be in.
 */
sealed class TrainingScriptProgress {

    /**
     * The script has not been started yet.
     */
    object NotStarted : TrainingScriptProgress()

    /**
     * The training is in progress.
     *
     * @param percentComplete The percent of epochs that have been completed.
     */
    @Serializable
    data class InProgress(val percentComplete: Double) : TrainingScriptProgress()

    /**
     * The training is finished.
     */
    object Completed : TrainingScriptProgress()
}

val trainingScriptProgressModule = SerializersModule {
    polymorphic<TrainingScriptProgress> {
        addSubclass(
            TrainingScriptProgress.NotStarted::class,
            ObjectSerializer(TrainingScriptProgress.NotStarted)
        )
        addSubclass(
            TrainingScriptProgress.InProgress::class,
            TrainingScriptProgress.InProgress.serializer()
        )
        addSubclass(
            TrainingScriptProgress.Completed::class,
            ObjectSerializer(TrainingScriptProgress.Completed)
        )
    }
}