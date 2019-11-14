package edu.wpi.axon.examplemodel

import arrow.fx.IO
import mu.KotlinLogging
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File
import java.nio.file.Paths

/**
 * An [ExampleModelManager] that pulls models from a Git repository.
 */
class GitExampleModelManager : ExampleModelManager {

    /**
     * The directory the example model cache lives in. Modify this to set a different cache
     * directory.
     */
    var cacheDir: File =
        Paths.get(System.getProperty("user.home"), ".wpilib", "Axon", "example-model-cache")
            .toFile()

    /**
     * The example model repository directory, inside [cacheDir].
     */
    val exampleModelRepoDir: File
        get() = Paths.get(cacheDir.absolutePath, "axon-example-models").toFile()

    /**
     * The remote that the example models are pulled from.
     */
    var exampleModelRepo = "https://github.com/wpilibsuite/axon-example-models.git"

    override fun getAllExampleModels(): IO<Set<ExampleModel>> = IO {
        check(exampleModelRepoDir.exists()) {
            "The example model cache (${exampleModelRepoDir.absolutePath}) is not on disk. " +
                "Try updating the cache."
        }

        val files = exampleModelRepoDir.listFiles()!!

        val metadata = ExampleModelsMetadata.deserialize(
            files.first { it.name == "exampleModels.json" }.readText()
        )

        metadata.exampleModels.forEach { exampleModel ->
            val modelFile = Paths.get(exampleModelRepoDir.absolutePath, exampleModel.path).toFile()
            check(modelFile.exists()) {
                "The model file $modelFile did not exist. Make sure the schema is correct."
            }
        }

        metadata.exampleModels
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun updateCache(): IO<Unit> = IO {
        // The cache dir either has to already exist or be created
        check(cacheDir.exists() || cacheDir.mkdirs()) {
            "Failed to make necessary cache directories in path: $cacheDir"
        }
    }.flatMap {
        IO {
            if (exampleModelRepoDir.exists()) {
                // The repo is on disk, pull to update it
                LOGGER.debug { "Repo dir $exampleModelRepoDir exists. Pulling." }
                RepositoryBuilder().findGitDir(exampleModelRepoDir).build().use { repo ->
                    Git(repo).use { git ->
                        git.pull().call()
                    }
                }
            } else {
                // The repo is not on disk, clone to get it
                LOGGER.debug { "Repo dir $exampleModelRepoDir does not exist. Cloning." }
                CloneCommand().setURI(exampleModelRepo)
                    .setDirectory(exampleModelRepoDir)
                    .call()
                    .close()
            }
            Unit
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
