package edu.wpi.inndie.ui.main

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import edu.wpi.inndie.aws.S3Manager
import edu.wpi.inndie.aws.S3PreferencesManager
import edu.wpi.inndie.aws.findINNDiES3Bucket
import edu.wpi.inndie.aws.plugin.S3PluginManager
import edu.wpi.inndie.aws.preferences.LocalPreferencesManager
import edu.wpi.inndie.db.JobDb
import edu.wpi.inndie.examplemodel.ExampleModelManager
import edu.wpi.inndie.examplemodel.GitExampleModelManager
import edu.wpi.inndie.plugin.DatasetPlugins.datasetPassthroughPlugin
import edu.wpi.inndie.plugin.DatasetPlugins.divideByTwoFiveFivePlugin
import edu.wpi.inndie.plugin.DatasetPlugins.processMnistTypeForMobilenetPlugin
import edu.wpi.inndie.plugin.DatasetPlugins.processMnistTypePlugin
import edu.wpi.inndie.plugin.LoadTestDataPlugins.loadExampleDatasetPlugin
import edu.wpi.inndie.plugin.LocalPluginManager
import edu.wpi.inndie.plugin.Plugin
import edu.wpi.inndie.plugin.PluginManager
import edu.wpi.inndie.plugin.ProcessTestOutputPlugins.autoMpgRegressionOutputPlugin
import edu.wpi.inndie.plugin.ProcessTestOutputPlugins.imageClassificationModelOutputPlugin
import edu.wpi.inndie.ui.JobLifecycleManager
import edu.wpi.inndie.ui.JobRunner
import edu.wpi.inndie.ui.ModelManager
import edu.wpi.inndie.util.datasetPluginManagerName
import edu.wpi.inndie.util.inndieBucketName
import edu.wpi.inndie.util.loadTestDataPluginManagerName
import edu.wpi.inndie.util.localCacheDir
import edu.wpi.inndie.util.processTestOutputPluginManagerName
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun defaultFrontendModule() = module {
    single(qualifier = named(inndieBucketName), createdAtStart = true) { findINNDiES3Bucket() }

    single {
        JobDb(
            Database.connect(
                url = "jdbc:h2:~/.wpilib/INNDiE/db;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver"
            )
        ).apply {
            // val modelName = "32_32_1_conv_sequential.h5"
            // val (model, path) = loadModel(modelName)
            //
            // create(
            //     name = "AWS Job",
            //     status = TrainingScriptProgress.NotStarted,
            //     userOldModelPath = ModelSource.FromFile(FilePath.S3(modelName)),
            //     userDataset = Dataset.ExampleDataset.FashionMnist,
            //     userOptimizer = Optimizer.Adam(
            //         learningRate = 0.001,
            //         beta1 = 0.9,
            //         beta2 = 0.999,
            //         epsilon = 1e-7,
            //         amsGrad = false
            //     ),
            //     userLoss = Loss.SparseCategoricalCrossentropy,
            //     userMetrics = setOf("accuracy"),
            //     userEpochs = 1,
            //     userNewModel = model,
            //     generateDebugComments = false,
            //     datasetPlugin = datasetPassthroughPlugin,
            //     internalTrainingMethod = InternalJobTrainingMethod.Untrained,
            //     target = ModelDeploymentTarget.Desktop
            // )
            //
            // create(
            //     name = "Local Job",
            //     status = TrainingScriptProgress.NotStarted,
            //     userOldModelPath = ModelSource.FromFile(FilePath.Local(path)),
            //     userDataset = Dataset.ExampleDataset.FashionMnist,
            //     userOptimizer = Optimizer.Adam(),
            //     userLoss = Loss.SparseCategoricalCrossentropy,
            //     userMetrics = setOf("accuracy"),
            //     userEpochs = 1,
            //     userNewModel = model,
            //     generateDebugComments = false,
            //     datasetPlugin = processMnistTypePlugin,
            //     internalTrainingMethod = InternalJobTrainingMethod.Untrained,
            //     target = ModelDeploymentTarget.Desktop
            // )
        }
    }

    single {
        when (val bucketName = get<Option<String>>(named(inndieBucketName))) {
            is Some -> S3PreferencesManager(S3Manager(bucketName.t)).apply { initialize() }
            is None -> LocalPreferencesManager(
                localCacheDir.resolve("preferences.json")
            ).apply { initialize() }
        }
    }

    single(named(datasetPluginManagerName)) {
        // TODO: Load official plugins from resources
        bindPluginManager(
            setOf(
                datasetPassthroughPlugin,
                processMnistTypePlugin,
                processMnistTypeForMobilenetPlugin,
                divideByTwoFiveFivePlugin
            ),
            "inndie-dataset-plugins",
            "dataset_plugin_cache.json"
        )
    }

    single(named(loadTestDataPluginManagerName)) {
        bindPluginManager(
            setOf(
                loadExampleDatasetPlugin
            ),
            "inndie-load-test-data-plugins",
            "load_test_data_plugin_cache.json"
        )
    }

    single(named(processTestOutputPluginManagerName)) {
        bindPluginManager(
            setOf(
                imageClassificationModelOutputPlugin,
                autoMpgRegressionOutputPlugin
            ),
            "inndie-process-test-output-plugins",
            "process_test_output_plugin_cache.json"
        )
    }

    single(createdAtStart = true) {
        // This needs to be eager so we eagerly resume tracking in-progress Jobs
        JobLifecycleManager(
            jobRunner = get(),
            jobDb = get(),
            waitAfterStartingJobMs = 5000L
        ).apply { initialize() }
    }

    single { ModelManager() }
    single { JobRunner() }
    single<ExampleModelManager> { GitExampleModelManager()
        .apply { updateCache().unsafeRunSync() } }
}

private fun Scope.bindPluginManager(
    officialPlugins: Set<Plugin.Official>,
    cacheName: String,
    cacheFileName: String
): PluginManager = when (val bucketName = get<Option<String>>(named(inndieBucketName))) {
    is Some -> {
        S3PluginManager(
            S3Manager(bucketName.t),
            cacheName,
            officialPlugins
        ).apply { initialize() }
    }

    is None -> LocalPluginManager(
        localCacheDir.resolve(cacheFileName).toFile(),
        officialPlugins
    ).apply { initialize() }
}
