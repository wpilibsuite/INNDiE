package edu.wpi.axon.ui

import arrow.core.None
import arrow.fx.IO
import arrow.fx.extensions.fx
import edu.wpi.axon.aws.RunTrainingScriptConfiguration
import edu.wpi.axon.aws.TrainingScriptRunner
import edu.wpi.axon.dbdata.Job
import edu.wpi.axon.tfdata.Model
import edu.wpi.axon.training.TrainGeneralModelScriptGenerator
import edu.wpi.axon.training.TrainSequentialModelScriptGenerator
import edu.wpi.axon.training.TrainState

/**
 * @param bucketName The S3 bucket name to use for dataset and models or `null` if AWS will not
 * be used.
 * @param scriptRunner The [TrainingScriptRunner] to run the script with.
 */
class JobRunner(
    private val bucketName: String?,
    private val scriptRunner: TrainingScriptRunner
) {

    /**
     * Generates the code for a job and starts it on EC2.
     *
     * @param job The [Job] to run.
     * @return The script id of the script that was started.
     */
    fun startJob(job: Job): Long = IO.fx {
        val trainModelScriptGenerator = when (val model = job.userModel) {
            is Model.Sequential -> TrainSequentialModelScriptGenerator(toTrainState(job, model))
            is Model.General -> TrainGeneralModelScriptGenerator(toTrainState(job, model))
        }

        val script = trainModelScriptGenerator.generateScript().fold(
            {
                IO.raiseError<String>(
                    IllegalStateException(
                        """
                        |Got errors when generating script:
                        |${it.all.joinToString("\n")}
                        """.trimMargin()
                    )
                )
            },
            { IO.just(it) }
        ).bind()

        scriptRunner.startScript(
            RunTrainingScriptConfiguration(
                oldModelName = trainModelScriptGenerator.trainState.userOldModelName,
                newModelName = job.userNewModelName,
                dataset = job.userDataset,
                scriptContents = script,
                epochs = job.userEpochs
            )
        ).bind()
    }.unsafeRunSync()

    fun getProgress(id: Long) = scriptRunner.getTrainingProgress(id)

    private fun <T : Model> toTrainState(
        job: Job,
        model: T
    ) = TrainState(
        userOldModelPath = job.userOldModelPath,
        userNewModelName = job.userNewModelName,
        userDataset = job.userDataset,
        userOptimizer = job.userOptimizer,
        userLoss = job.userLoss,
        userMetrics = job.userMetrics,
        userEpochs = job.userEpochs,
        userValidationSplit = None, // TODO: Add this to Job and pull it from there
        userNewModel = model,
        userBucketName = bucketName,
        handleS3InScript = false,
        generateDebugComments = job.generateDebugComments
    )
}
