package edu.wpi.inndie.tfdata.layer

import edu.wpi.inndie.tfdata.Model
import edu.wpi.inndie.tfdata.SerializableEitherITii
import edu.wpi.inndie.tfdata.SerializableTuple2II
import kotlinx.serialization.Serializable

/**
 * A TensorFlow layer.
 *
 * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers
 */
@Serializable
sealed class Layer {

    /**
     * The unique name of this layer.
     */
    abstract val name: String

    /**
     * Any inputs to this layer. Should be [None] for Sequential models and [Some] for other
     * models. Each element is the [name] of another layer.
     */
    abstract val inputs: Set<String>?

    /**
     * @param trainable Whether this layer should be trained.
     * @return A new [MetaLayer.TrainableLayer] that wraps this layer.
     */
    fun isTrainable(trainable: Boolean = true) =
        MetaLayer.TrainableLayer(
            name,
            inputs,
            this,
            trainable
        )

    /**
     * @return A new [MetaLayer.UntrainableLayer] that wraps this layer.
     */
    fun untrainable() =
        MetaLayer.UntrainableLayer(
            name,
            inputs,
            this
        )

    abstract fun copyWithNewInputs(inputs: Set<String>): Layer

    /**
     * Adds some information and delegates to another [Layer].
     */
    @Serializable
    sealed class MetaLayer : Layer() {

        abstract val layer: Layer

        abstract override fun copyWithNewInputs(inputs: Set<String>): MetaLayer

        /**
         * A layer which is trainable.
         *
         * @param trainable Whether this layer should be trained.
         */
        @Serializable
        data class TrainableLayer(
            override val name: String,
            override val inputs: Set<String>?,
            override val layer: Layer,
            val trainable: Boolean
        ) : MetaLayer() {
            init {
                require(layer !is MetaLayer)
            }

            override fun copyWithNewInputs(inputs: Set<String>) =
                copy(inputs = inputs, layer = layer.copyWithNewInputs(inputs))
        }

        /**
         * A layer which is untrainable. This should not be confused with a [TrainableLayer]
         * where [TrainableLayer.isTrainable] is `true`. An [UntrainableLayer] is IMPOSSIBLE to train.
         */
        @Serializable
        data class UntrainableLayer(
            override val name: String,
            override val inputs: Set<String>?,
            override val layer: Layer
        ) : MetaLayer() {
            init {
                require(layer !is MetaLayer)
            }

            override fun copyWithNewInputs(inputs: Set<String>): UntrainableLayer {
                return if (layer is InputLayer) {
                    // InputLayers can't have inputs, so if we get one, make sure there are no
                    // inputs. After that, there is nothing new in the copy so just return a normal
                    // copy.
                    check(inputs.isEmpty())
                    copy()
                } else {
                    // All the other layers can have inputs so copy the layer with the new inputs.
                    copy(inputs = inputs, layer = layer.copyWithNewInputs(inputs))
                }
            }
        }
    }

    /**
     * A placeholder layer for a layer that INNDiE does not understand.
     */
    @Serializable
    data class UnknownLayer(
        override val name: String,
        override val inputs: Set<String>?
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * A layer that contains an entire model inside it.
     *
     * @param model The model that acts as this layer.
     */
    @Serializable
    data class ModelLayer(
        override val name: String,
        override val inputs: Set<String>?,
        val model: Model
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * A layer that accepts input data and has no parameters.
     *
     * // TODO: tensor parameter
     */
    @Serializable
    data class InputLayer
    private constructor(
        override val name: String,
        val batchInputShape: List<Int?>,
        val batchSize: Int? = null,
        val dtype: Double? = null,
        val sparse: Boolean = false
    ) : Layer() {

        override val inputs: Set<String>? = null

        fun toInputData(): Model.General.InputData =
            Model.General.InputData(name, batchInputShape, batchSize, dtype, sparse)

        override fun copyWithNewInputs(inputs: Set<String>) = error("An input layer has no inputs.")

        companion object {
            operator fun invoke(
                name: String,
                shape: List<Int?>,
                batchSize: Int? = null,
                dtype: Double? = null,
                sparse: Boolean = false
            ) = InputLayer(
                name,
                shape,
                batchSize,
                dtype,
                sparse
            ).untrainable()
        }
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/BatchNormalization
     *
     * Does not support the `adjustment` parameter.
     */
    @Serializable
    data class BatchNormalization(
        override val name: String,
        override val inputs: Set<String>?,
        val axis: Int = -1,
        val momentum: Double = 0.99,
        val epsilon: Double = 0.001,
        val center: Boolean = true,
        val scale: Boolean = true,
        val betaInitializer: Initializer = Initializer.Zeros,
        val gammaInitializer: Initializer = Initializer.Ones,
        val movingMeanInitializer: Initializer = Initializer.Zeros,
        val movingVarianceInitializer: Initializer = Initializer.Ones,
        val betaRegularizer: Regularizer? = null,
        val gammaRegularizer: Regularizer? = null,
        val betaConstraint: Constraint? = null,
        val gammaConstraint: Constraint? = null,
        val renorm: Boolean = false,
        val renormClipping: Map<String, Double>? = null,
        val renormMomentum: Double? = 0.99,
        val fused: Boolean? = null,
        val virtualBatchSize: Int? = null
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/AveragePooling2D
     */
    @Serializable
    data class AveragePooling2D(
        override val name: String,
        override val inputs: Set<String>?,
        val poolSize: SerializableEitherITii =
            SerializableEitherITii.Right(
                SerializableTuple2II(
                    2,
                    2
                )
            ),
        val strides: SerializableEitherITii? = null,
        val padding: PoolingPadding = PoolingPadding.Valid,
        val dataFormat: DataFormat? = null
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Conv2D
     */
    @Serializable
    data class Conv2D(
        override val name: String,
        override val inputs: Set<String>?,
        val filters: Int,
        val kernel: SerializableTuple2II,
        val activation: Activation
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Dense
     */
    @Serializable
    data class Dense(
        override val name: String,
        override val inputs: Set<String>?,
        val units: Int,
        val activation: Activation = Activation.Linear,
        val useBias: Boolean = true,
        val kernelInitializer: Initializer = Initializer.GlorotUniform(
            null
        ),
        val biasInitializer: Initializer = Initializer.Zeros,
        val kernelRegularizer: Regularizer? = null,
        val biasRegularizer: Regularizer? = null,
        val activityRegularizer: Regularizer? = null,
        val kernelConstraint: Constraint? = null,
        val biasConstraint: Constraint? = null
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Dropout
     */
    @Serializable
    data class Dropout(
        override val name: String,
        override val inputs: Set<String>?,
        val rate: Double,
        val noiseShape: List<Int>? = null,
        val seed: Int? = null
    ) : Layer() {

        init {
            require(rate in 0.0..1.0) {
                "rate ($rate) was outside the allowed range of [0, 1]."
            }
        }

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Flatten
     */
    @Serializable
    data class Flatten(
        override val name: String,
        override val inputs: Set<String>?,
        val dataFormat: DataFormat? = null
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/GlobalAveragePooling2D
     */
    @Serializable
    data class GlobalAveragePooling2D(
        override val name: String,
        override val inputs: Set<String>?,
        val dataFormat: DataFormat?
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/GlobalMaxPool2D
     */
    @Serializable
    data class GlobalMaxPooling2D(
        override val name: String,
        override val inputs: Set<String>?,
        val dataFormat: DataFormat? = null
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/MaxPool2D
     */
    @Serializable
    data class MaxPooling2D(
        override val name: String,
        override val inputs: Set<String>?,
        val poolSize: SerializableEitherITii =
            SerializableEitherITii.Right(
                SerializableTuple2II(
                    2,
                    2
                )
            ),
        val strides: SerializableEitherITii? = null,
        val padding: PoolingPadding = PoolingPadding.Valid,
        val dataFormat: DataFormat? = null
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/SpatialDropout2D
     */
    @Serializable
    data class SpatialDropout2D(
        override val name: String,
        override val inputs: Set<String>?,
        val rate: Double,
        val dataFormat: DataFormat? = null
    ) : Layer() {

        init {
            require(rate in 0.0..1.0) {
                "rate ($rate) was outside the allowed range of [0, 1]."
            }
        }

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/UpSampling2D
     *
     * Bug: TF does not export a value for [interpolation].
     */
    @Serializable
    data class UpSampling2D(
        override val name: String,
        override val inputs: Set<String>?,
        val size: SerializableEitherITii =
            SerializableEitherITii.Right(
                SerializableTuple2II(
                    2,
                    2
                )
            ),
        val dataFormat: DataFormat? = null,
        val interpolation: Interpolation = Interpolation.Nearest
    ) : Layer() {

        override fun copyWithNewInputs(inputs: Set<String>) = copy(inputs = inputs)
    }
}
