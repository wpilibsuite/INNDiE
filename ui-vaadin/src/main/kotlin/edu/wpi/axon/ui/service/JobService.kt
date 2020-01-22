package edu.wpi.axon.ui.service

import arrow.core.Either
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider
import com.vaadin.flow.data.provider.Query
import edu.wpi.axon.db.JobDb
import edu.wpi.axon.dbdata.Job
import edu.wpi.axon.dbdata.TrainingScriptProgress
import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.Model
import edu.wpi.axon.tfdata.loss.Loss
import edu.wpi.axon.tfdata.optimizer.Optimizer
import edu.wpi.axon.tflayerloader.ModelLoaderFactory
import edu.wpi.axon.ui.JobRunner
import java.io.File
import java.nio.file.Paths
import java.util.stream.Stream
import org.jetbrains.exposed.sql.Database
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.model.InstanceType

object JobService {
    class JobProvider : AbstractBackEndDataProvider<Job, Void>() {
        override fun sizeInBackEnd(query: Query<Job, Void>?): Int {
            return jobs.count()
        }

        override fun fetchFromBackEnd(query: Query<Job, Void>): Stream<Job> {
            return jobs.fetch(query.limit, query.offset).stream()
        }

        override fun getId(item: Job): Any {
            return item.id
        }
    }

    val jobs = JobDb(
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
    )

    val dataProvider = JobProvider()

    val jobRunner = JobRunner(
        "axon-autogenerated-5bl5pyn1h8g73kxak0xsnacql02e9i",
        InstanceType.T2_MICRO,
        Region.US_EAST_1
    )

    private fun loadModel(modelName: String): Pair<Model, String> {
        val localModelPath = Paths.get("/home/salmon/Documents/Axon/training/src/test/resources/edu/wpi/axon/training/$modelName").toString()
        val layers = ModelLoaderFactory().createModeLoader(localModelPath).load(File(localModelPath))
        val model = layers.attempt().unsafeRunSync()
        check(model is Either.Right)
        return model.b to localModelPath
    }

    init {
        val newModelName = "32_32_1_conv_sequential-trained.h5"
        val (model, path) = loadModel("32_32_1_conv_sequential.h5")
        jobs.create(
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
//        for (i in 1..20) {
//            jobs.create(Random.nextJob())
//        }
    }
}
