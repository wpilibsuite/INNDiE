package edu.wpi.axon.ui.controller

import edu.wpi.axon.db.JobDb
import edu.wpi.axon.db.JobDbOp
import edu.wpi.axon.ui.model.JobDto
import javafx.application.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tornadofx.Controller
import tornadofx.asObservable
import tornadofx.toObservable

class JobBoard : Controller() {
    private val database by di<JobDb>()

    val jobs by lazy {
        database.fetchAll().map { JobDto(it) }.asObservable()
    }

    init {
        database.subscribe { op, job ->
            Platform.runLater {
                when (op) {
                    JobDbOp.Create -> jobs.add(JobDto(job))

                    JobDbOp.Update -> jobs.first { it.id == job.id }.apply {
                        name = job.name
                        status = job.status
                        userOldModelPath = job.userOldModelPath
                        userDataset = job.userDataset
                        userOptimizer = job.userOptimizer
                        optimizerType = job.userOptimizer::class
                        userLoss = job.userLoss
                        lossType = job.userLoss::class
                        userMetrics = job.userMetrics.toObservable()
                        userEpochs = job.userEpochs
                        userNewModel = job.userNewModel
                        target = job.target
                        targetType = job.target::class
                        datasetPlugin = job.datasetPlugin
                        id = job.id
                    }

                    JobDbOp.Remove -> jobs.removeIf { it.id == job.id }
                }
            }
        }
    }
}
