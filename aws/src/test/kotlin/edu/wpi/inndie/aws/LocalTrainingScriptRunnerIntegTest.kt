package edu.wpi.inndie.aws

import edu.wpi.inndie.db.data.TrainingScriptProgress
import edu.wpi.inndie.tfdata.Dataset
import edu.wpi.inndie.util.FilePath
import edu.wpi.inndie.util.getOutputModelName
import io.kotlintest.matchers.booleans.shouldBeFalse
import io.kotlintest.matchers.file.shouldExist
import io.kotlintest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir

internal class LocalTrainingScriptRunnerIntegTest {

    private val runner = LocalTrainingScriptRunner()

    @Test
    @Timeout(value = 10L, unit = TimeUnit.MINUTES)
    @Tag("needsTensorFlowSupport")
    fun `test running mnist training script`(@TempDir tempDir: File) {
        val oldModelName = "custom_fashion_mnist.h5"
        val oldModelPath = this::class.java.getResource(oldModelName).path
        val newModelPath = tempDir.toPath().resolve("custom_fashion_mnist-trained.h5")
        val id = 1
        runner.startScript(
            RunTrainingScriptConfiguration(
                FilePath.Local(oldModelPath),
                Dataset.ExampleDataset.Mnist,
                """
                import tensorflow as tf
                import os
                import errno
                from pathlib import Path

                var10 = tf.keras.models.load_model("$oldModelPath")

                var12 = tf.keras.Sequential([
                    var10.get_layer("conv2d_4"),
                    var10.get_layer("conv2d_5"),
                    var10.get_layer("max_pooling2d_2"),
                    var10.get_layer("dropout_4"),
                    var10.get_layer("flatten_2"),
                    var10.get_layer("dense_4"),
                    var10.get_layer("dropout_5"),
                    var10.get_layer("dense_5")
                ])
                var12.get_layer("conv2d_4").trainable = False
                var12.get_layer("conv2d_5").trainable = False
                var12.get_layer("max_pooling2d_2").trainable = False
                var12.get_layer("dropout_4").trainable = False
                var12.get_layer("flatten_2").trainable = False
                var12.get_layer("dense_4").trainable = True
                var12.get_layer("dropout_5").trainable = True
                var12.get_layer("dense_5").trainable = True

                var12.compile(
                    optimizer=tf.keras.optimizers.Adam(0.001, 0.9, 0.999, 1.0E-7, False),
                    loss=tf.keras.losses.sparse_categorical_crossentropy,
                    metrics=["accuracy"]
                )

                try:
                    os.makedirs(Path("$tempDir/sequential_2-weights.{epoch:02d}-{val_loss:.2f}.hdf5").parent)
                except OSError as err:
                    if err.errno != errno.EEXIST:
                        raise

                var15 = tf.keras.callbacks.ModelCheckpoint(
                    "$tempDir/sequential_2-weights.{epoch:02d}-{val_loss:.2f}.hdf5",
                    monitor="val_loss",
                    verbose=1,
                    save_best_only=False,
                    save_weights_only=True,
                    mode="auto",
                    save_freq="epoch",
                    load_weights_on_restart=False
                )

                var17 = tf.keras.callbacks.EarlyStopping(
                    monitor="val_loss",
                    min_delta=0,
                    patience=10,
                    verbose=1,
                    mode="auto",
                    baseline=None,
                    restore_best_weights=False
                )

                (var1, var2), (var3, var4) = tf.keras.datasets.mnist.load_data()

                var6 = var1.reshape(-1, 28, 28, 1) / 255

                var8 = var3.reshape(-1, 28, 28, 1) / 255

                csvLogger = tf.keras.callbacks.CSVLogger(
                    "$tempDir/trainingLog.csv",
                    separator=',',
                    append=False
                )

                var12.fit(
                    var6,
                    var2,
                    batch_size=None,
                    epochs=2,
                    verbose=2,
                    callbacks=[var15, var17, csvLogger],
                    validation_data=(var8, var4),
                    shuffle=True
                )

                try:
                    os.makedirs(Path("$newModelPath").parent)
                except OSError as err:
                    if err.errno != errno.EEXIST:
                        raise

                var12.save("$newModelPath")
                """.trimIndent(),
                1,
                tempDir.toPath(),
                id
            )
        )

        println(tempDir.walkTopDown().joinToString("\n"))

        while (true) {
            val progress = runner.getTrainingProgress(id)
            println(progress)
            if (progress == TrainingScriptProgress.Completed) {
                println(tempDir.walkTopDown().joinToString("\n"))
                newModelPath.shouldExist()
                break // Done with test
            } else if (progress is TrainingScriptProgress.Error) {
                fail { "Progress was: $progress" }
            }
            Thread.sleep(1000)
        }
    }

    @Test
    @Timeout(value = 10L, unit = TimeUnit.MINUTES)
    @Tag("needsTensorFlowSupport")
    fun `test cancelling mnist training script`(@TempDir tempDir: File) {
        val oldModelName = "custom_fashion_mnist.h5"
        val oldModelPath = this::class.java.getResource(oldModelName).path
        val newModelPath = tempDir.toPath().resolve("custom_fashion_mnist-trained.h5").toString()
        val id = 1
        runner.startScript(
            RunTrainingScriptConfiguration(
                FilePath.Local(oldModelPath),
                Dataset.ExampleDataset.Mnist,
                """
                import tensorflow as tf
                import os
                import errno
                from pathlib import Path

                var10 = tf.keras.models.load_model("$oldModelPath")

                var12 = tf.keras.Sequential([
                    var10.get_layer("conv2d_4"),
                    var10.get_layer("conv2d_5"),
                    var10.get_layer("max_pooling2d_2"),
                    var10.get_layer("dropout_4"),
                    var10.get_layer("flatten_2"),
                    var10.get_layer("dense_4"),
                    var10.get_layer("dropout_5"),
                    var10.get_layer("dense_5")
                ])
                var12.get_layer("conv2d_4").trainable = False
                var12.get_layer("conv2d_5").trainable = False
                var12.get_layer("max_pooling2d_2").trainable = False
                var12.get_layer("dropout_4").trainable = False
                var12.get_layer("flatten_2").trainable = False
                var12.get_layer("dense_4").trainable = True
                var12.get_layer("dropout_5").trainable = True
                var12.get_layer("dense_5").trainable = True

                var12.compile(
                    optimizer=tf.keras.optimizers.Adam(0.001, 0.9, 0.999, 1.0E-7, False),
                    loss=tf.keras.losses.sparse_categorical_crossentropy,
                    metrics=["accuracy"]
                )

                try:
                    os.makedirs(Path("./sequential_2-weights.{epoch:02d}-{val_loss:.2f}.hdf5").parent)
                except OSError as err:
                    if err.errno != errno.EEXIST:
                        raise

                var15 = tf.keras.callbacks.ModelCheckpoint(
                    "./sequential_2-weights.{epoch:02d}-{val_loss:.2f}.hdf5",
                    monitor="val_loss",
                    verbose=1,
                    save_best_only=False,
                    save_weights_only=True,
                    mode="auto",
                    save_freq="epoch",
                    load_weights_on_restart=False
                )

                var17 = tf.keras.callbacks.EarlyStopping(
                    monitor="val_loss",
                    min_delta=0,
                    patience=10,
                    verbose=1,
                    mode="auto",
                    baseline=None,
                    restore_best_weights=False
                )

                (var1, var2), (var3, var4) = tf.keras.datasets.mnist.load_data()

                var6 = var1.reshape(-1, 28, 28, 1) / 255

                var8 = var3.reshape(-1, 28, 28, 1) / 255

                var12.fit(
                    var6,
                    var2,
                    batch_size=None,
                    epochs=1,
                    verbose=2,
                    callbacks=[var15, var17],
                    validation_data=(var8, var4),
                    shuffle=True
                )

                try:
                    os.makedirs(Path("./custom_fashion_mnist-trained.h5").parent)
                except OSError as err:
                    if err.errno != errno.EEXIST:
                        raise

                var12.save("./custom_fashion_mnist-trained.h5")
                """.trimIndent(),
                1,
                tempDir.toPath(),
                id
            )
        )

        while (true) {
            when (runner.getTrainingProgress(id)) {
                TrainingScriptProgress.Completed ->
                    fail { "The script should not have completed yet." }

                is TrainingScriptProgress.Error ->
                    fail { "The script should not have errored yet." }

                TrainingScriptProgress.Creating,
                TrainingScriptProgress.Initializing,
                is TrainingScriptProgress.InProgress -> {
                    runner.cancelScript(id)
                    val progressAfterCancellation = runner.getTrainingProgress(id)
                    progressAfterCancellation.shouldBeInstanceOf<TrainingScriptProgress.Error>()
                    tempDir.toPath().resolve(
                        getOutputModelName(
                            oldModelName
                        )
                    ).toFile()
                        .exists().shouldBeFalse()
                    return // Done with the test
                }
            }
            Thread.sleep(1000)
        }
    }
}
