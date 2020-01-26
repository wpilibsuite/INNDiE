package edu.wpi.axon.training

import arrow.core.NonEmptyList
import arrow.core.Validated
import arrow.core.invalidNel
import arrow.fx.IO
import com.google.common.base.Throwables
import edu.wpi.axon.dsl.ScriptGenerator
import edu.wpi.axon.dsl.container.DefaultPolymorphicNamedDomainObjectContainer
import edu.wpi.axon.dsl.creating
import edu.wpi.axon.dsl.running
import edu.wpi.axon.dsl.task.ApplySequentialLayerDeltaTask
import edu.wpi.axon.dsl.variable.Variable
import edu.wpi.axon.tfdata.Model
import mu.KotlinLogging

/**
 * Trains a [Model.Sequential].
 *
 * @param trainState The train state to pull all the configuration data from.
 */
@Suppress("UNUSED_VARIABLE")
class TrainSequentialModelScriptGenerator(
    override val trainState: TrainState<Model.Sequential>,
    private val oldModel: Model.Sequential
) : TrainModelScriptGenerator<Model.Sequential> {

    init {
        require(trainState.userOldModelPath.filename != trainState.userNewModelPath.filename) {
            "The old model name (${trainState.userOldModelPath}) cannot equal the new model " +
                "name (${trainState.userNewModelPath})."
        }
    }

    override fun generateScript(): Validated<NonEmptyList<String>, String> {
        LOGGER.info {
            "Generating script with trainState:\n$trainState"
        }

        return IO {
            require(trainState.userNewModel.batchInputShape.count { it == null } <= 1)
            val reshapeArgsFromBatchShape =
                trainState.userNewModel.batchInputShape.map { it ?: -1 }

            val script = ScriptGenerator(
                DefaultPolymorphicNamedDomainObjectContainer.of(),
                DefaultPolymorphicNamedDomainObjectContainer.of()
            ) {
                val loadedDataset = reshapeAndScaleLoadedDataset(
                    loadDataset(trainState),
                    reshapeArgsFromBatchShape,
                    255
                )

                val model = loadModel(trainState)

                val newModel by variables.creating(Variable::class)
                val applyLayerDeltaTask by tasks.running(ApplySequentialLayerDeltaTask::class) {
                    modelInput = model
                    oldLayers = oldModel.layers
                    newLayers = trainState.userNewModel.layers
                    newModelOutput = newModel
                }

                lastTask = compileTrainSave(
                    trainState,
                    oldModel,
                    newModel,
                    applyLayerDeltaTask,
                    loadedDataset
                )
            }

            script.code(trainState.generateDebugComments)
        }.attempt().unsafeRunSync().fold(
            { Throwables.getStackTraceAsString(it).invalidNel() },
            { it }
        )
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
