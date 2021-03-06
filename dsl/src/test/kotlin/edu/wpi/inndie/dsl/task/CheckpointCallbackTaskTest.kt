package edu.wpi.inndie.dsl.task

import edu.wpi.inndie.dsl.configuredCorrectly
import edu.wpi.inndie.testutil.KoinTestFixture
import edu.wpi.inndie.tfdata.ModelCheckpointSaveFrequency
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin

internal class CheckpointCallbackTaskTest : KoinTestFixture() {

    @Test
    fun `test code gen`() {
        startKoin { }

        val task = CheckpointCallbackTask("").apply {
            filePath = "weights.{epoch:02d}-{val_loss:.2f}.hdf5"
            monitor = "val_loss"
            verbose = 0
            saveBestOnly = false
            saveWeightsOnly = false
            mode = "auto"
            saveFrequency = ModelCheckpointSaveFrequency.Epoch
            loadWeightsOnRestart = false
            output = configuredCorrectly("output")
        }

        task.code() shouldBe """
            |try:
            |    os.makedirs(Path("weights.{epoch:02d}-{val_loss:.2f}.hdf5").parent)
            |except OSError as err:
            |    if err.errno != errno.EEXIST:
            |        raise
            |
            |output = tf.keras.callbacks.ModelCheckpoint(
            |    "weights.{epoch:02d}-{val_loss:.2f}.hdf5",
            |    monitor="val_loss",
            |    verbose=0,
            |    save_best_only=False,
            |    save_weights_only=False,
            |    mode="auto",
            |    save_freq="epoch",
            |    load_weights_on_restart=False
            |)
        """.trimMargin()
    }
}
