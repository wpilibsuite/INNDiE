package edu.wpi.axon.ui

import arrow.core.Either
import edu.wpi.axon.aws.EC2TrainingScriptRunner
import edu.wpi.axon.aws.S3PreferencesManager
import edu.wpi.axon.aws.TrainingScriptRunner
import edu.wpi.axon.aws.axonBucketName
import edu.wpi.axon.aws.findAxonS3Bucket
import edu.wpi.axon.aws.preferences.LocalPreferencesManager
import edu.wpi.axon.aws.preferences.PreferencesManager
import edu.wpi.axon.db.JobDb
import edu.wpi.axon.dbdata.Job
import edu.wpi.axon.dbdata.TrainingScriptProgress
import edu.wpi.axon.dsl.defaultModule
import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.Model
import edu.wpi.axon.tfdata.loss.Loss
import edu.wpi.axon.tfdata.optimizer.Optimizer
import edu.wpi.axon.tflayerloader.DefaultLayersToGraph
import edu.wpi.axon.tflayerloader.LoadLayersFromHDF5
import edu.wpi.axon.ui.service.JobProvider
import java.io.File
import java.nio.file.Paths
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

@WebListener
class WebAppListener : ServletContextListener {

    override fun contextInitialized(sce: ServletContextEvent?) {
        LOGGER.info { "Starting web app." }

        // Find the S3 bucket that Axon is going to work out of
        val bucketName: String? = findAxonS3Bucket()

        val preferencesManager: PreferencesManager =
            if (bucketName != null) {
                // We are using AWS
                S3PreferencesManager(bucketName).apply { initialize() }
            } else {
                // We are not using AWS
                LocalPreferencesManager(
                        Paths.get(
                                System.getProperty("user.home"),
                                ".wpilib",
                                "Axon",
                                "edu.wpi.axon.aws.preferences.json"
                        )
                ).apply { initialize() }
            }

        // TODO: Don't hardcode an in-memory db
        val jobDb = JobDb(
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver"
            )
        )

        val dataProvider = JobProvider(jobDb)

        startKoin {
            modules(listOf(
                defaultModule(),
                module {
                    single(named(axonBucketName)) { bucketName }
                    single { dataProvider }
                    single { jobDb }
                    single { preferencesManager }
                    factory<TrainingScriptRunner> {
                        val bucketName = get<String?>(named(axonBucketName))
                        if (bucketName != null) {
                            EC2TrainingScriptRunner(bucketName, get<PreferencesManager>().get().defaultEC2NodeType)
                        } else {
                            TODO("Support running outside of AWS. Create a local training script runner")
                        }
                    }
                }
            ))
        }

        val newModelName = "32_32_1_conv_sequential-trained.h5"
        val (model, path) = loadModel("32_32_1_conv_sequential.h5")
        jobDb.create(
            Job(
                "Job 1",
                TrainingScriptProgress.NotStarted,
                path,
                newModelName,
                Dataset.ExampleDataset.FashionMnist,
                Optimizer.Adam(0.001, 0.9, 0.999, 1e-7, false),
                Loss.SparseCategoricalCrossentropy,
                setOf("accuracy"),
                1,
                model,
                false
            )
        )
    }

    override fun contextDestroyed(sce: ServletContextEvent?) {
        LOGGER.info { "Stopping web app." }
    }

    private fun loadModel(modelName: String): Pair<Model, String> {
        val localModelPath =
            Paths.get("/Users/AustinShalit/git/Axon/training/src/test/resources/edu/wpi/axon/training/$modelName")
                .toString()
        val layers = LoadLayersFromHDF5(DefaultLayersToGraph()).load(File(localModelPath))
        val model = layers.attempt().unsafeRunSync()
        check(model is Either.Right)
        return model.b to localModelPath
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
