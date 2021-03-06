package edu.wpi.inndie.ui

import arrow.core.Option
import arrow.core.getOrElse
import edu.wpi.inndie.aws.S3Manager
import edu.wpi.inndie.db.data.ModelSource
import edu.wpi.inndie.examplemodel.ExampleModelManager
import edu.wpi.inndie.tfdata.Model
import edu.wpi.inndie.tflayerloader.ModelLoaderFactory
import edu.wpi.inndie.util.FilePath
import edu.wpi.inndie.util.inndieBucketName
import java.io.File
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named

class ModelManager : KoinComponent {

    private val bucketName by inject<Option<String>>(named(inndieBucketName))
    private val s3Manager by lazy { bucketName.map { S3Manager(it) } }
    private val exampleModelManager by inject<ExampleModelManager>()

    fun downloadModel(modelSource: ModelSource): FilePath.Local {
        return when (modelSource) {
            is ModelSource.FromExample -> {
                val file = exampleModelManager.download(modelSource.exampleModel).unsafeRunSync()
                FilePath.Local(file.absolutePath)
            }

            is ModelSource.FromFile -> when (modelSource.filePath) {
                is FilePath.S3 -> {
                    val file = getS3Manager().downloadModel(modelSource.filePath.path)
                    FilePath.Local(file.absolutePath)
                }

                is FilePath.Local -> modelSource.filePath as FilePath.Local
            }
        }
    }

    fun uploadModel(modelSource: ModelSource): FilePath.S3 {
        return when (modelSource) {
            is ModelSource.FromExample -> {
                val file = downloadModel(modelSource)
                getS3Manager().uploadModel(File(file.path))
                FilePath.S3(file.filename)
            }

            is ModelSource.FromFile -> when (modelSource.filePath) {
                is FilePath.S3 -> modelSource.filePath as FilePath.S3
                is FilePath.Local -> {
                    getS3Manager().uploadModel(File(modelSource.filePath.path))
                    FilePath.S3(modelSource.filePath.filename)
                }
            }
        }
    }

    fun loadModel(modelSource: ModelSource): Model {
        val localFile = File(downloadModel(modelSource).path)
        return ModelLoaderFactory().createModelLoader(localFile.name)
            .load(localFile)
            .unsafeRunSync()
    }

    private fun getS3Manager(): S3Manager = s3Manager.getOrElse {
        error("Must have an S3Manager.")
    }
}
