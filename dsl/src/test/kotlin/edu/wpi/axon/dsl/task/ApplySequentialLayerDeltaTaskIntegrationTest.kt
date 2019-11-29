@file:SuppressWarnings(
    "TooManyFunctions",
    "StringLiteralDuplication",
    "LargeClass",
    "LongMethod",
    "SpreadOperator"
)

package edu.wpi.axon.dsl.task

import edu.wpi.axon.dsl.configuredCorrectly
import edu.wpi.axon.dsl.defaultModule
import edu.wpi.axon.testutil.KoinTestFixture
import edu.wpi.axon.tfdata.layer.Activation
import edu.wpi.axon.tfdata.layer.Layer
import io.kotlintest.matchers.booleans.shouldBeFalse
import io.kotlintest.matchers.booleans.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin

internal class ApplySequentialLayerDeltaTaskIntegrationTest : KoinTestFixture() {

    @Test
    fun `keep all 1 layers`() {
        startKoin {
            modules(defaultModule())
        }

        val layer1 = Layer.Dense("dense_1", null, 10, Activation.ReLu).trainable()
        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf(layer1)
            newLayers = setOf(layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.isConfiguredCorrectly().shouldBeTrue()
        task.code() shouldBe """
            |new_model = tf.keras.Sequential([base_model.get_layer("dense_1")])
            |new_model.get_layer("dense_1").trainable = True
        """.trimMargin()
    }

    @Test
    fun `keep all 2 layers`() {
        startKoin {
            modules(defaultModule())
        }

        val layer1 = Layer.Dense("dense_1", null, 10, Activation.ReLu).trainable()
        val layer2 = Layer.UnknownLayer("unknown_1", null).trainable()
        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf(layer1, layer2)
            newLayers = setOf(layer1, layer2)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([
            |    base_model.get_layer("dense_1"),
            |    base_model.get_layer("unknown_1")
            |])
            |new_model.get_layer("dense_1").trainable = True
            |new_model.get_layer("unknown_1").trainable = True
        """.trimMargin()
    }

    @Test
    fun `remove one layer`() {
        startKoin {
            modules(defaultModule())
        }

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers =
                setOf(Layer.Dense("dense_1", null, 10, Activation.ReLu).trainable())
            newLayers = setOf()
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([])
            |
        """.trimMargin()
    }

    @Test
    fun `remove two layers`() {
        startKoin {
            modules(defaultModule())
        }

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf(
                Layer.Dense("dense_1", null, 10, Activation.ReLu).trainable(),
                Layer.UnknownLayer("unknown_1", null).trainable()
            )
            newLayers = setOf()
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([])
            |
        """.trimMargin()
    }

    @Test
    @SuppressWarnings("MaxLineLength")
    fun `add one layer`() {
        startKoin {
            modules(defaultModule())
        }

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf()
            newLayers = setOf(Layer.Dense("dense_1", null, 10, Activation.ReLu).trainable())
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([tf.keras.layers.Dense(units=10, activation=tf.keras.activations.relu, name="dense_1")])
            |new_model.get_layer("dense_1").trainable = True
        """.trimMargin()
    }

    @Test
    fun `add two layers`() {
        startKoin {
            modules(defaultModule())
        }

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf()
            newLayers = setOf(
                Layer.Dense("dense_1", null, 128, Activation.ReLu).trainable(),
                Layer.Dense("dense_2", null, 10, Activation.SoftMax).trainable()
            )
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([
            |    tf.keras.layers.Dense(units=128, activation=tf.keras.activations.relu, name="dense_1"),
            |    tf.keras.layers.Dense(units=10, activation=tf.keras.activations.softmax, name="dense_2")
            |])
            |new_model.get_layer("dense_1").trainable = True
            |new_model.get_layer("dense_2").trainable = True
        """.trimMargin()
    }

    @Test
    fun `add an unknown layer`() {
        startKoin {
            modules(defaultModule())
        }

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf()
            newLayers = setOf(Layer.UnknownLayer("layer_1", null).trainable())
            newModelOutput = configuredCorrectly("new_model")
        }

        shouldThrow<IllegalStateException> { task.code() }
    }

    @Test
    fun `add layer with an unknown activation function`() {
        startKoin {
            modules(defaultModule())
        }

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf()
            newLayers = setOf(
                Layer.Dense(
                    "dense_1",
                    null,
                    128,
                    Activation.UnknownActivation("activation_1")
                ).trainable()
            )
            newModelOutput = configuredCorrectly("new_model")
        }

        shouldThrow<IllegalArgumentException> { task.code() }
    }

    @Test
    fun `remove the first layer and replace the second`() {
        startKoin {
            modules(defaultModule())
        }

        val layer1 = Layer.UnknownLayer("unknown_3", null).trainable()
        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf(
                layer1,
                Layer.Dense("dense_2", null, 10, Activation.SoftMax).trainable()
            )
            newLayers = setOf(
                layer1,
                Layer.Dense("dense_2", null, 3, Activation.SoftMax).trainable()
            )
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([
            |    base_model.get_layer("unknown_3"),
            |    tf.keras.layers.Dense(units=3, activation=tf.keras.activations.softmax, name="dense_2")
            |])
            |new_model.get_layer("unknown_3").trainable = True
            |new_model.get_layer("dense_2").trainable = True
        """.trimMargin()
    }

    @Test
    fun `copy an unknown layer`() {
        startKoin {
            modules(defaultModule())
        }

        val layer1 = Layer.UnknownLayer("unknown_1", null).trainable()
        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf(layer1)
            newLayers = setOf(layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([base_model.get_layer("unknown_1")])
            |new_model.get_layer("unknown_1").trainable = True
        """.trimMargin()
    }

    @Test
    fun `copy a layer with an unknown activation function`() {
        startKoin {
            modules(defaultModule())
        }

        val layer1 = Layer.Dense(
            "dense_1",
            null,
            10,
            Activation.UnknownActivation("activation_1")
        ).trainable()

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf(layer1)
            newLayers = setOf(layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([base_model.get_layer("dense_1")])
            |new_model.get_layer("dense_1").trainable = True
        """.trimMargin()
    }

    @Test
    fun `copy a layer that is only different in the trainable flag`() {
        startKoin {
            modules(defaultModule())
        }

        val baseLayer1 = Layer.Dense("dense_1", null, 10, Activation.ReLu)
        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf(baseLayer1.trainable())
            newLayers = setOf(baseLayer1.trainable(false))
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([base_model.get_layer("dense_1")])
            |new_model.get_layer("dense_1").trainable = False
        """.trimMargin()
    }

    @Test
    fun `add a non-Sequential layer`() {
        startKoin {
            modules(defaultModule())
        }

        val task = ApplySequentialLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            oldLayers = setOf()
            newLayers = setOf(
                Layer.Dense(
                    "dense_1",
                    setOf("input_name"),
                    10,
                    Activation.ReLu
                ).trainable()
            )
            newModelOutput = configuredCorrectly("new_model")
        }

        task.isConfiguredCorrectly().shouldBeFalse()
    }
}
