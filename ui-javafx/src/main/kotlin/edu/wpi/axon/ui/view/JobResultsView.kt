package edu.wpi.axon.ui.view

import edu.wpi.axon.db.data.TrainingScriptProgress
import edu.wpi.axon.ui.JobLifecycleManager
import edu.wpi.axon.ui.model.JobModel
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode
import javafx.scene.layout.VBox
import tornadofx.Fragment
import tornadofx.borderpane
import tornadofx.bottom
import tornadofx.label
import tornadofx.linechart
import tornadofx.listview
import tornadofx.multiseries
import tornadofx.objectBinding
import tornadofx.paddingAll
import tornadofx.series
import tornadofx.textarea
import tornadofx.toObservable

class JobResultsView : Fragment() {

    private val job by inject<JobModel>()
    private val jobLifecycleManager by di<JobLifecycleManager>()

    override val root = borderpane {
        centerProperty().bind(job.status.objectBinding { progress ->
            when (progress) {
                TrainingScriptProgress.Completed -> VBox().apply {
                    spacing = 10.0

                    val jobResults = job.id.objectBinding {
                        if (it == null) {
                            emptyList()
                        } else {
                            jobLifecycleManager.listResults(it.toInt())
                        }.toObservable()
                    }

                    linechart(
                        "Training Results",
                        x = NumberAxis(),
                        y = NumberAxis()
                    ) {
                        xAxis.label = "Epoch"

                        val trainingLogFile = jobLifecycleManager.getResult(
                            job.id.value.toInt(),
                            jobResults.value!!.first { it == "trainingLog.csv" }
                        )
                        val trainingLogData = trainingLogFile.readLines()
                        val colNames = trainingLogData.first().split(',')
                        multiseries(names = *colNames.mapNotNull {
                            if (it == "epoch") null else it
                        }.toTypedArray()) {
                            val epochIndex = colNames.indexOf("epoch")
                            trainingLogData.drop(1).forEach {
                                val values = it.split(',')
                                data(
                                    values[epochIndex].toInt(),
                                    *values.mapIndexedNotNull { index, data ->
                                        if (index == epochIndex) null
                                        else data.toDouble()
                                    }.toTypedArray()
                                )
                            }
                        }
                    }

                    listview<String> {
                        itemsProperty().bind(jobResults)
                        selectionModel.selectionMode = SelectionMode.SINGLE
                        isEditable = false
                        selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                            if (newValue == null) {
                                bottom = null
                            } else {
                                bottom {
                                    add(
                                        find<ResultFragment>(
                                            mapOf(
                                                ResultFragment::data to LazyResult(
                                                    newValue,
                                                    lazy {
                                                        jobLifecycleManager.getResult(
                                                            job.id.value.toInt(),
                                                            newValue
                                                        )
                                                    }
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                is TrainingScriptProgress.Error -> VBox().apply {
                    spacing = 10.0
                    label("Job completed erroneously. Error Log:")
                    textarea {
                        text = progress.log
                        isEditable = false
                        isWrapText = true
                    }
                }

                TrainingScriptProgress.NotStarted -> Label("Job has not been started yet.")
                TrainingScriptProgress.Creating, TrainingScriptProgress.Initializing -> Label("Job is starting.")
                is TrainingScriptProgress.InProgress -> Label("Job has not finished training yet.")
                null -> Label("No Job selected.")
            }.apply {
                paddingAll = 10
            }
        })
    }
}
