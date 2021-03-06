package edu.wpi.inndie.aws

interface TrainingScriptRunner : TrainingScriptProgressReporter, TrainingScriptCanceller,
    TrainingResultSupplier {

    /**
     * Start running a training script.
     *
     * @param config The data needed to start the script.
     */
    fun startScript(config: RunTrainingScriptConfiguration)
}
