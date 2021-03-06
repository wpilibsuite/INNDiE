@file:SuppressWarnings("LongMethod", "LargeClass")

package edu.wpi.inndie.training

import arrow.core.None
import edu.wpi.inndie.dsl.defaultBackendModule
import edu.wpi.inndie.dsl.task.RunEdgeTpuCompilerTask
import edu.wpi.inndie.plugin.DatasetPlugins.datasetPassthroughPlugin
import edu.wpi.inndie.plugin.DatasetPlugins.processMnistTypePlugin
import edu.wpi.inndie.testutil.KoinTestFixture
import edu.wpi.inndie.tfdata.Dataset
import edu.wpi.inndie.tfdata.Model
import edu.wpi.inndie.tfdata.loss.Loss
import edu.wpi.inndie.tfdata.optimizer.Optimizer
import edu.wpi.inndie.training.testutil.loadModel
import edu.wpi.inndie.training.testutil.testTrainingScript
import edu.wpi.inndie.util.FilePath
import edu.wpi.inndie.util.inndieBucketName
import io.kotlintest.assertions.arrow.validation.shouldBeValid
import io.kotlintest.matchers.file.shouldExist
import io.kotlintest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Paths
import kotlin.random.Random
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal class TrainSequentialModelScriptGeneratorIntegrationTest : KoinTestFixture() {

    @Test
    @Tag("needsTensorFlowSupport")
    fun `test with fashion mnist`(@TempDir tempDir: File) {
        startKoin {
            modules(
                listOf(
                    defaultBackendModule(),
                    module {
                        single(named(inndieBucketName)) { "dummy-bucket-name" }
                    }
                )
            )
        }

        val modelName = "custom_fashion_mnist.h5"
        val newModelName = tempDir.toPath().resolve("custom_fashion_mnist-trained.h5").toString()
        val (model, path) = loadModel(modelName) {}
        model.shouldBeInstanceOf<Model.Sequential> {
            TrainSequentialModelScriptGenerator(
                TrainState(
                    userOldModelPath = FilePath.Local(path),
                    userDataset = Dataset.ExampleDataset.Mnist,
                    userOptimizer = Optimizer.Adam(0.001, 0.9, 0.999, 1e-7, false),
                    userLoss = Loss.SparseCategoricalCrossentropy,
                    userMetrics = setOf("accuracy"),
                    userEpochs = 1,
                    userNewModel = it.copy(
                        layers = it.layers.mapIndexedTo(mutableSetOf()) { index, layer ->
                            // Only train the last 3 layers
                            if (it.layers.size - index <= 3) layer.layer.isTrainable()
                            else layer.layer.isTrainable(false)
                        }),
                    userValidationSplit = None,
                    generateDebugComments = false,
                    target = ModelDeploymentTarget.Desktop,
                    datasetPlugin = processMnistTypePlugin,
                    workingDir = tempDir.toPath(),
                    jobId = Random.nextInt(1, Int.MAX_VALUE)
                ),
                it
            ).generateScript().shouldBeValid { (script) ->
                testTrainingScript(
                    tempDir,
                    script,
                    newModelName
                )
            }
        }
    }

    @Test
    @Tag("needsTensorFlowSupport")
    fun `test with fashion mnist targeting the coral`(@TempDir tempDir: File) {
        startKoin {
            modules(
                listOf(
                    defaultBackendModule(),
                    module {
                        single(named(inndieBucketName)) { "dummy-bucket-name" }
                    }
                )
            )
        }

        val modelName = "custom_fashion_mnist.h5"
        val newModelName = tempDir.toPath().resolve("custom_fashion_mnist-trained.h5").toString()
        val (model, path) = loadModel(modelName) {}
        model.shouldBeInstanceOf<Model.Sequential> {
            TrainSequentialModelScriptGenerator(
                TrainState(
                    userOldModelPath = FilePath.Local(path),
                    userDataset = Dataset.ExampleDataset.Mnist,
                    userOptimizer = Optimizer.Adam(0.001, 0.9, 0.999, 1e-7, false),
                    userLoss = Loss.SparseCategoricalCrossentropy,
                    userMetrics = setOf("accuracy"),
                    userEpochs = 1,
                    userNewModel = it.copy(
                        layers = it.layers.mapIndexedTo(mutableSetOf()) { index, layer ->
                            // Only train the last 3 layers
                            if (it.layers.size - index <= 3) layer.layer.isTrainable()
                            else layer.layer.isTrainable(false)
                        }),
                    userValidationSplit = None,
                    generateDebugComments = false,
                    target = ModelDeploymentTarget.Coral(0.0001),
                    workingDir = tempDir.toPath(),
                    datasetPlugin = processMnistTypePlugin,
                    jobId = Random.nextInt(1, Int.MAX_VALUE)
                ),
                it
            ).generateScript().shouldBeValid { (script) ->
                testTrainingScript(
                    tempDir,
                    script,
                    newModelName
                )
            }
        }
    }

    @Test
    @Tag("needsTensorFlowSupport")
    fun `test small model with reduced wpilib dataset`(@TempDir tempDir: File) {
        startKoin {
            modules(defaultBackendModule())
        }

        // TODO: This breaks at runtime with a Coral target
        val modelName = "small_model_for_wpilib_reduced_dataset.h5"
        val newModelName =
            tempDir.toPath().resolve("small_model_for_wpilib_reduced_dataset-trained.h5").toString()
        val (model, path) = loadModel(modelName) {}
        model.shouldBeInstanceOf<Model.Sequential> {
            TrainSequentialModelScriptGenerator(
                TrainState(
                    userOldModelPath = FilePath.Local(path),
                    userDataset = Dataset.Custom(
                        FilePath.Local("WPILib_reduced.tar"),
                        "WPILib reduced"
                    ),
                    userOptimizer = Optimizer.Adam(0.001, 0.9, 0.999, 1e-7, false),
                    userLoss = Loss.SparseCategoricalCrossentropy,
                    userMetrics = setOf("accuracy"),
                    userEpochs = 1,
                    userNewModel = it.copy(
                        layers = it.layers.mapIndexedTo(mutableSetOf()) { index, layer ->
                            // Only train the last layer
                            if (it.layers.size - index <= 1) layer.layer.isTrainable()
                            else layer.layer.isTrainable(false)
                        }),
                    userValidationSplit = None,
                    generateDebugComments = false,
                    target = ModelDeploymentTarget.Desktop,
                    workingDir = tempDir.toPath(),
                    datasetPlugin = datasetPassthroughPlugin,
                    jobId = Random.nextInt(1, Int.MAX_VALUE)
                ),
                it
            ).generateScript().shouldBeValid { (script) ->
                Paths.get(this::class.java.getResource("WPILib_reduced.tar").toURI()).toFile()
                    .copyTo(Paths.get(tempDir.absolutePath, "WPILib_reduced.tar").toFile())
                testTrainingScript(
                    tempDir,
                    script,
                    newModelName
                )
            }
        }
    }

    @Test
    @Tag("needsTensorFlowSupport")
    @Disabled("Broken targeting the Coral.")
    fun `test small model with reduced wpilib dataset targeting the coral`(@TempDir tempDir: File) {
        startKoin {
            modules(defaultBackendModule())
        }

        val modelName = "small_model_for_wpilib_reduced_dataset.h5"
        val newModelName =
            tempDir.toPath().resolve("small_model_for_wpilib_reduced_dataset-trained.h5").toString()
        val (model, path) = loadModel(modelName) {}
        model.shouldBeInstanceOf<Model.Sequential> {
            TrainSequentialModelScriptGenerator(
                TrainState(
                    userOldModelPath = FilePath.Local(path),
                    userDataset = Dataset.Custom(
                        FilePath.Local("WPILib_reduced.tar"),
                        "WPILib reduced"
                    ),
                    userOptimizer = Optimizer.Adam(0.001, 0.9, 0.999, 1e-7, false),
                    userLoss = Loss.SparseCategoricalCrossentropy,
                    userMetrics = setOf("accuracy"),
                    userEpochs = 1,
                    userNewModel = it.copy(
                        layers = it.layers.mapIndexedTo(mutableSetOf()) { index, layer ->
                            // Only train the last layer
                            if (it.layers.size - index <= 1) layer.layer.isTrainable()
                            else layer.layer.isTrainable(false)
                        }),
                    userValidationSplit = None,
                    generateDebugComments = false,
                    target = ModelDeploymentTarget.Coral(0.0001),
                    workingDir = tempDir.toPath(),
                    datasetPlugin = datasetPassthroughPlugin,
                    jobId = Random.nextInt(1, Int.MAX_VALUE)
                ),
                it
            ).generateScript().shouldBeValid { (script) ->
                Paths.get(this::class.java.getResource("WPILib_reduced.tar").toURI()).toFile()
                    .copyTo(Paths.get(tempDir.absolutePath, "WPILib_reduced.tar").toFile())
                testTrainingScript(
                    tempDir,
                    script,
                    newModelName
                )
                // Also test for the compiled output
                tempDir.toPath().resolve(
                    RunEdgeTpuCompilerTask.getEdgeTpuCompiledModelFilename(newModelName)
                ).shouldExist()
            }
        }
    }
}
