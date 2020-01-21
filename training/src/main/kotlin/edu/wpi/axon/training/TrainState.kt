package edu.wpi.axon.training

import arrow.core.None
import arrow.core.Option
import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.Model
import edu.wpi.axon.tfdata.loss.Loss
import edu.wpi.axon.tfdata.optimizer.Optimizer
import java.nio.file.Paths

/**
 * All configuration data needed to generate a training script.
 *
 * @param userOldModelPath The path to the model to load.
 * @param userNewModelName The name of the model to save to.
 * @param userDataset The dataset to train on.
 * @param userOptimizer The [Optimizer] to use.
 * @param userLoss The [Loss] function to use.
 * @param userMetrics Any metrics.
 * @param userEpochs The number of epochs.
 * @param userNewModel The new model.
 * @param userBucketName The name of the S3 bucket Axon uses as a cache, or `null` if AWS will not
 * be used.
 * @param handleS3InScript Whether to download/upload the model from/to S3 inside the generated
 * script. This causes the script to make calls to S3 ON ITS OWN. This parameter should not be
 * confused with using EC2TrainingScriptRunner, which handles S3 independently of the script (i.e.,
 * when using EC2TrainingScriptRunner, the script should not try to deal with S3 itself).
 * @param generateDebugComments Whether to put debug comments in the output.
 */
data class TrainState<T : Model>(
    val userOldModelPath: String,
    val userNewModelName: String,
    val userDataset: Dataset,
    val userOptimizer: Optimizer,
    val userLoss: Loss,
    val userMetrics: Set<String>,
    val userEpochs: Int,
    val userNewModel: T,
    val userValidationSplit: Option<Double> = None,
    val userBucketName: String? = null,
    val handleS3InScript: Boolean = false,
    val generateDebugComments: Boolean = false
) {

    /**
     * The filename (with extension) of the old model.
     */
    val userOldModelName: String
        get() = Paths.get(userOldModelPath).fileName.toString()
}
