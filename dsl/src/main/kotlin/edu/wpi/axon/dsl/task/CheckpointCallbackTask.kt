package edu.wpi.axon.dsl.task

import edu.wpi.axon.dsl.Code
import edu.wpi.axon.dsl.imports.Import
import edu.wpi.axon.dsl.imports.makeImport
import edu.wpi.axon.dsl.variable.Variable
import edu.wpi.axon.tfdata.ModelCheckpointSaveFrequency
import edu.wpi.axon.tfdata.code.boolToPythonString
import edu.wpi.axon.tfdata.code.numberToPythonString
import edu.wpi.axon.util.singleAssign

/**
 * Makes a new `ModelCheckpoint` callback.
 */
class CheckpointCallbackTask(name: String) : BaseTask(name) {

    /**
     * The formatted file path.
     * https://www.tensorflow.org/api_docs/python/tf/keras/callbacks/ModelCheckpoint
     */
    var filePath: String by singleAssign()

    /**
     * The quantity to monitor.
     */
    var monitor = "val_loss"

    /**
     * The verbosity mode: `0` or `1`.
     */
    var verbose = 0

    /**
     * If `true`, the latest best model will not be overwritten.
     */
    var saveBestOnly = false

    /**
     * One of `auto`, `min`, `max`. Used to determine when to overwrite the current save file
     * if [saveBestOnly] is `true`.
     */
    var mode = "auto"

    /**
     * If `true`, only the model's weights will be saved. Else, the full model will be saved.
     */
    var saveWeightsOnly = false

    /**
     * How frequently to make checkpoints.
     */
    var saveFrequency: ModelCheckpointSaveFrequency = ModelCheckpointSaveFrequency.Epoch

    /**
     * If `true`, the model will attempt to load the checkpoint file at the start of training.
     */
    var loadWeightsOnRestart = false

    /**
     * Where to save the callback to.
     */
    var output: Variable by singleAssign()

    override val imports: Set<Import> = setOf(makeImport("import tensorflow as tf"))

    override val inputs: Set<Variable> = setOf()

    override val outputs: Set<Variable>
        get() = setOf(output)

    override val dependencies: Set<Code<*>> = setOf()

    override fun code() = """
        |${output.name} = tf.keras.callbacks.ModelCheckpoint(
        |    "$filePath",
        |    monitor="$monitor",
        |    verbose=${numberToPythonString(verbose)},
        |    save_best_only=${boolToPythonString(saveBestOnly)},
        |    save_weights_only=${boolToPythonString(saveWeightsOnly)},
        |    mode="$mode",
        |    save_freq=$saveFrequency,
        |    load_weights_on_restart=${boolToPythonString(loadWeightsOnRestart)}
        |)
    """.trimMargin()
}
