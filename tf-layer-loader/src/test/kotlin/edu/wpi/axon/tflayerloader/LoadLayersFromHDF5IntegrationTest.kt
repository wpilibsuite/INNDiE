@file:SuppressWarnings("LongMethod", "LargeClass")
@file:Suppress("UnstableApiUsage")

package edu.wpi.axon.tflayerloader

import arrow.core.None
import arrow.core.Right
import arrow.core.Some
import arrow.core.Tuple2
import edu.wpi.axon.tfdata.Model
import edu.wpi.axon.tfdata.layer.Activation
import edu.wpi.axon.tfdata.layer.Constraint
import edu.wpi.axon.tfdata.layer.DataFormat
import edu.wpi.axon.tfdata.layer.PoolingPadding
import edu.wpi.axon.tfdata.layer.Regularizer
import edu.wpi.axon.tfdata.layer.SealedLayer
import edu.wpi.axon.tfdata.layer.trainable
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.matchers.booleans.shouldBeTrue
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

internal class LoadLayersFromHDF5IntegrationTest {

    @Test
    fun `load from test file 1`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("model1.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.Sequential> {
                it.name shouldBe "sequential_11"
                it.batchInputShape.shouldContainExactly(null, 28, 28, 1)
                it.layers.shouldContainExactly(
                    setOf(
                        SealedLayer.Conv2D(
                            "conv2d_16",
                            None,
                            32,
                            Tuple2(3, 3),
                            Activation.ReLu
                        ).trainable(),
                        SealedLayer.Conv2D(
                            "conv2d_17",
                            None,
                            64,
                            Tuple2(3, 3),
                            Activation.ReLu
                        ).trainable(),
                        SealedLayer.MaxPooling2D(
                            "max_pooling2d_8",
                            None,
                            Right(Tuple2(2, 2)),
                            Right(Tuple2(2, 2)),
                            PoolingPadding.Valid,
                            DataFormat.ChannelsLast
                        ).trainable(),
                        SealedLayer.Dropout(
                            "dropout_19",
                            None,
                            0.25
                        ).trainable(),
                        SealedLayer.Flatten(
                            "flatten_8",
                            None,
                            DataFormat.ChannelsLast
                        ).trainable(),
                        SealedLayer.Dense(
                            "dense_22",
                            None,
                            128,
                            Activation.ReLu
                        ).trainable(),
                        SealedLayer.Dropout(
                            "dropout_20",
                            None,
                            0.5
                        ).trainable(),
                        SealedLayer.Dense(
                            "dense_23",
                            None,
                            10,
                            Activation.SoftMax
                        ).trainable()
                    )
                )
            }
        }
    }

    @Test
    fun `load from test file 2`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("mobilenetv2_1.00_224.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.General> {
                it.name shouldBe "mobilenetv2_1.00_224"
                it.input.shouldContainExactly(
                    Model.General.InputData("input_1", listOf(224, 224, 3))
                )
                it.output.shouldContainExactly(Model.General.OutputData("Logits"))
                it.layers.nodes() shouldHaveSize 157

                val nodesWithMultipleInputs = it.layers.nodes().filter {
                    it.inputs is Some && (it.inputs as Some).t.size > 1
                }

                // Only the block_xx_add layers should have more than one input
                nodesWithMultipleInputs.all {
                    it.name.startsWith("block_") && it.name.endsWith("_add")
                }.shouldBeTrue()
            }
        }
    }

    @Test
    fun `load from bad file`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("badModel1.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeLeft()
    }

    @Test
    fun `load non-sequential model 1`() {
        val layers = setOf(
            SealedLayer.InputLayer("input_2", listOf(3)),
            SealedLayer.Dense(
                "dense_2",
                Some(setOf("input_2")),
                4,
                Activation.ReLu
            ).trainable(), SealedLayer.Dense(
                "dense_3",
                Some(setOf("dense_2")),
                5,
                Activation.SoftMax
            ).trainable()
        )

        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("nonSequentialModel1.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.General> {
                it.name shouldBe "model_1"
                it.input.shouldContainExactly(Model.General.InputData("input_2", listOf(3)))
                it.output.shouldContainExactly(Model.General.OutputData("dense_3"))
                it.layers.nodes() shouldContainExactlyInAnyOrder layers
            }
        }
    }

    @Test
    fun `load rnn 1`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("rnn1.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.General> {
                it.name shouldBe "model_5"
                it.input.shouldContainExactly(
                    Model.General.InputData(
                        "input_15",
                        listOf(null, 5)
                    )
                )
                it.output.shouldContainExactly(Model.General.OutputData("dense_1"))
                it.layers.nodes().shouldContainExactly(
                    // TODO: Add an RNN layer class
                    SealedLayer.InputLayer("input_15", listOf(null, 5)),
                    SealedLayer.UnknownLayer("rnn_12", Some(setOf("input_15"))).trainable(),
                    SealedLayer.Dense(
                        "dense_1",
                        Some(setOf("rnn_12")),
                        10,
                        Activation.SoftMax
                    ).trainable()
                )
            }
        }
    }

    @Test
    fun `load sequential with l1 regularizer`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("sequential_with_l1_regularizer.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.Sequential> {
                it.name shouldBe "sequential_9"
                it.batchInputShape shouldBe listOf(null, 1)
                it.layers.shouldContainExactly(
                    SealedLayer.Dense(
                        "dense_3",
                        None,
                        1,
                        Activation.Linear,
                        kernelRegularizer = Regularizer.L1L2(0.01, 0.0)
                    ).trainable()
                )
            }
        }
    }

    @Test
    fun `load sequential with maxnorm constraint`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("sequential_with_maxnorm_constraint.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.Sequential> {
                it.name shouldBe "sequential_12"
                it.batchInputShape shouldBe listOf(null, 1)
                it.layers.shouldContainExactly(
                    SealedLayer.Dense(
                        "dense_6",
                        None,
                        1,
                        Activation.Linear,
                        kernelConstraint = Constraint.MaxNorm(2.0, 0)
                    ).trainable()
                )
            }
        }
    }

    @Test
    fun `load sequential with minmaxnorm constraint`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("sequential_with_minmaxnorm_constraint.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.Sequential> {
                it.name shouldBe "sequential_13"
                it.batchInputShape shouldBe listOf(null, 1)
                it.layers.shouldContainExactly(
                    SealedLayer.Dense(
                        "dense_7",
                        None,
                        1,
                        Activation.Linear,
                        kernelConstraint = Constraint.MinMaxNorm(1.0, 2.0, 3.0, 0)
                    ).trainable()
                )
            }
        }
    }

    @Test
    fun `load sequential with unitnorm constraint`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("sequential_with_unitnorm_constraint.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.Sequential> {
                it.name shouldBe "sequential_15"
                it.batchInputShape shouldBe listOf(null, 1)
                it.layers.shouldContainExactly(
                    SealedLayer.Dense(
                        "dense_9",
                        None,
                        1,
                        Activation.Linear,
                        kernelConstraint = Constraint.UnitNorm(0)
                    ).trainable()
                )
            }
        }
    }

    @Test
    fun `load sequential with nonneg constraint`() {
        LoadLayersFromHDF5(DefaultLayersToGraph()).load(
            File(this::class.java.getResource("sequential_with_nonneg_constraint.h5").toURI())
        ).attempt().unsafeRunSync().shouldBeRight { model ->
            model.shouldBeInstanceOf<Model.Sequential> {
                it.name shouldBe "sequential_14"
                it.batchInputShape shouldBe listOf(null, 1)
                it.layers.shouldContainExactly(
                    SealedLayer.Dense(
                        "dense_8",
                        None,
                        1,
                        Activation.Linear,
                        kernelConstraint = Constraint.NonNeg
                    ).trainable()
                )
            }
        }
    }
}
