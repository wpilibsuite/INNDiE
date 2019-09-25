package edu.wpi.axon.training

import edu.wpi.axon.dsl.defaultModule
import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.Model
import edu.wpi.axon.tfdata.loss.Loss
import edu.wpi.axon.tfdata.optimizer.Optimizer
import edu.wpi.axon.tflayerloader.DefaultLayersToGraph
import edu.wpi.axon.tflayerloader.LoadLayersFromHDF5
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.assertions.arrow.validation.shouldBeValid
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import java.io.File

internal class TrainGeneralIntegrationTest {

    @Test
    fun `test with custom model with an add`() {
        startKoin {
            modules(defaultModule())
        }

        val modelName = "network_with_add.h5"
        val localModelPath = TrainGeneralIntegrationTest::class.java
            .getResource(modelName).toURI().path
        val layers = LoadLayersFromHDF5(DefaultLayersToGraph()).load(File(localModelPath))

        layers.attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.General> {
                TrainGeneral(
                    userModelPath = localModelPath,
                    userDataset = Dataset.Mnist,
                    userOptimizer = Optimizer.Adam(0.001, 0.9, 0.999, 1e-7, false),
                    userLoss = Loss.SparseCategoricalCrossentropy,
                    userMetrics = setOf("accuracy"),
                    userEpochs = 50,
                    userNewLayers = it.layers.nodes()
                ).generateScript().shouldBeValid {
                    println(it.a)
                    it.a shouldBe """
                    |import tensorflow as tf
                    |
                    |model = tf.keras.models.load_model("$modelName")
                    |
                    |input1 = tf.keras.layers.Input(
                    |newModel.get_layer("conv2d_6").trainable = False
                    |newModel.get_layer("conv2d_7").trainable = False
                    |newModel.get_layer("max_pooling2d_3").trainable = False
                    |newModel.get_layer("dropout_6").trainable = False
                    |newModel.get_layer("flatten_3").trainable = False
                    |newModel.get_layer("dense_6").trainable = True
                    |newModel.get_layer("dropout_7").trainable = True
                    |newModel.get_layer("dense_7").trainable = True
                    |
                    |checkpointCallback = tf.keras.callbacks.ModelCheckpoint(
                    |    "sequential_3-weights.{epoch:02d}-{val_loss:.2f}.hdf5",
                    |    monitor="val_loss",
                    |    verbose=1,
                    |    save_best_only=False,
                    |    save_weights_only=True,
                    |    mode="auto",
                    |    save_freq="epoch",
                    |    load_weights_on_restart=False
                    |)
                    |
                    |newModel.compile(
                    |    optimizer=tf.keras.optimizers.Adam(0.001, 0.9, 0.999, 1.0E-7, False),
                    |    loss=tf.keras.losses.sparse_categorical_crossentropy,
                    |    metrics=["accuracy"]
                    |)
                    |
                    |earlyStoppingCallback = tf.keras.callbacks.EarlyStopping(
                    |    monitor="val_loss",
                    |    min_delta=0,
                    |    patience=10,
                    |    verbose=1,
                    |    mode="auto",
                    |    baseline=None,
                    |    restore_best_weights=False
                    |)
                    |
                    |(xTrain, yTrain), (xTest, yTest) = tf.keras.datasets.mnist.load_data()
                    |
                    |scaledXTest = xTest.reshape(-1, 28, 28, 1) / 255
                    |
                    |scaledXTrain = xTrain.reshape(-1, 28, 28, 1) / 255
                    |
                    |newModel.fit(
                    |    scaledXTrain,
                    |    yTrain,
                    |    batch_size=None,
                    |    epochs=50,
                    |    verbose=2,
                    |    callbacks=[checkpointCallback, earlyStoppingCallback],
                    |    validation_data=(scaledXTest, yTest),
                    |    shuffle=True
                    |)
                    """.trimMargin()
                }
            }
        }
    }
}
