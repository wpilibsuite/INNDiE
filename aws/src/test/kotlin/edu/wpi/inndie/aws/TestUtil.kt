package edu.wpi.inndie.aws

import edu.wpi.inndie.db.data.nextDataset
import edu.wpi.inndie.util.FilePath
import java.io.File
import kotlin.random.Random
import org.apache.commons.lang3.RandomStringUtils

internal fun randomRunTrainingScriptConfigurationUsingAWS(tempDir: File) = RunTrainingScriptConfiguration(
    oldModelName = FilePath.S3("${RandomStringUtils.randomAlphanumeric(10)}.h5"),
    dataset = Random.nextDataset(),
    scriptContents = RandomStringUtils.randomAlphanumeric(10),
    epochs = Random.nextInt(1, 10),
    workingDir = tempDir.toPath(),
    id = Random.nextInt()
)
