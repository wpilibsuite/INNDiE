package edu.wpi.axon.dsl.task

import edu.wpi.axon.dsl.configuredCorrectly
import edu.wpi.axon.dsl.defaultUniqueVariableNameGenerator
import edu.wpi.axon.testutil.KoinTestFixture
import edu.wpi.axon.tflayer.python.LayerToCode
import edu.wpi.axon.tflayers.Activation
import edu.wpi.axon.tflayers.Layer
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module

internal class ApplyLayerDeltaTaskTest : KoinTestFixture() {

    @Test
    fun `keep all 1 layers`() {
        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
            })
        }

        val layer1 = Layer.Dense("dense_1", true, 10, Activation.ReLu)

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf(layer1)
            newLayers = listOf(layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([base_model.get_layer("dense_1")])
        """.trimMargin()
    }

    @Test
    fun `keep all 2 layers`() {
        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
            })
        }

        val layer1 = Layer.Dense("dense_1", true, 10, Activation.ReLu)
        val layer2 = Layer.UnknownLayer("unknown_1", true)

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf(layer1, layer2)
            newLayers = listOf(layer1, layer2)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([
            |    base_model.get_layer("dense_1"),
            |    base_model.get_layer("unknown_1")
            |])
        """.trimMargin()
    }

    @Test
    fun `remove one layer`() {
        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
            })
        }

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf(Layer.Dense("dense_1", true, 10, Activation.ReLu))
            newLayers = listOf()
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([])
        """.trimMargin()
    }

    @Test
    fun `remove two layers`() {
        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
            })
        }

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf(
                Layer.Dense("dense_1", true, 10, Activation.ReLu),
                Layer.UnknownLayer("unknown_1", true)
            )
            newLayers = listOf()
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([])
        """.trimMargin()
    }

    @Test
    fun `add one layer`() {
        val layer1 = Layer.Dense("dense_1", true, 10, Activation.ReLu)

        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
                single<LayerToCode> {
                    mockk {
                        every { makeNewLayer(layer1) } returns "layer1"
                    }
                }
            })
        }

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf()
            newLayers = listOf(layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([layer1])
        """.trimMargin()
    }

    @Test
    fun `add two layers`() {
        val layer1 = Layer.Dense("dense_1", true, 128, Activation.ReLu)
        val layer2 = Layer.Dense("dense_2", true, 10, Activation.SoftMax)

        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
                single<LayerToCode> {
                    mockk {
                        every { makeNewLayer(layer1) } returns "layer1"
                        every { makeNewLayer(layer2) } returns "layer2"
                    }
                }
            })
        }

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf()
            newLayers = listOf(layer1, layer2)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([
            |    layer1,
            |    layer2
            |])
        """.trimMargin()
    }

    @Test
    fun `remove the first layer and replace the second and swap them`() {
        val layer1 = Layer.UnknownLayer("unknown_3", true)
        val layer2Old = Layer.Dense("dense_2", true, 10, Activation.SoftMax)
        val layer2New = Layer.Dense("dense_2", true, 3, Activation.SoftMax)

        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
                single<LayerToCode> {
                    mockk {
                        every { makeNewLayer(layer2New) } returns "layer2New"
                    }
                }
            })
        }

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf(layer1, layer2Old)
            newLayers = listOf(layer2New, layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([
            |    layer2New,
            |    base_model.get_layer("unknown_3")
            |])
        """.trimMargin()
    }

    @Test
    fun `copy an unknown layer`() {
        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
            })
        }

        val layer1 = Layer.UnknownLayer("unknown_1", true)

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf(layer1)
            newLayers = listOf(layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([base_model.get_layer("unknown_1")])
        """.trimMargin()
    }

    @Test
    fun `copy a layer with an unknown activation function`() {
        val layer1 = Layer.Dense("dense_1", true, 10, Activation.UnknownActivation("activation_1"))

        startKoin {
            modules(module {
                defaultUniqueVariableNameGenerator()
            })
        }

        val task = ApplyLayerDeltaTask("task1").apply {
            modelInput = configuredCorrectly("base_model")
            currentLayers = listOf(layer1)
            newLayers = listOf(layer1)
            newModelOutput = configuredCorrectly("new_model")
        }

        task.code() shouldBe """
            |new_model = tf.keras.Sequential([base_model.get_layer("dense_1")])
        """.trimMargin()
    }
}
