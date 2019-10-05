package edu.wpi.axon.tfdata.layer

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Right
import arrow.core.Tuple2
import edu.wpi.axon.tfdata.Model

/**
 * A sealed [Layer] implementation.
 */
sealed class SealedLayer : Layer {

    /**
     * Adds some information and delegates to another [SealedLayer].
     */
    sealed class MetaLayer(open val layer: SealedLayer) : SealedLayer() {

        /**
         * A layer which is trainable.
         *
         * @param trainable Whether this layer should be trained.
         */
        data class TrainableLayer(
            override val name: String,
            override val layer: SealedLayer,
            val trainable: Boolean
        ) : MetaLayer(layer), Layer by layer {
            init {
                require(layer !is MetaLayer)
            }
        }

        /**
         * A layer which is untrainable. This should not be confused with a [TrainableLayer]
         * where [TrainableLayer.trainable] is `true`. An [UntrainableLayer] is IMPOSSIBLE to train.
         */
        data class UntrainableLayer(
            override val name: String,
            override val layer: SealedLayer
        ) : MetaLayer(layer), Layer by layer {
            init {
                require(layer !is MetaLayer)
            }
        }
    }

    /**
     * A placeholder layer for a layer that Axon does not understand.
     */
    data class UnknownLayer(
        override val name: String,
        override val inputs: Option<Set<String>>
    ) : SealedLayer()

    /**
     * A layer that accepts input data and has no parameters.
     *
     * // TODO: tensor parameter
     */
    data class InputLayer
    private constructor(
        override val name: String,
        val batchInputShape: List<Int?>,
        val batchSize: Int? = null,
        val dtype: Number? = null,
        val sparse: Boolean = false
    ) : SealedLayer() {

        override val inputs: Option<Set<String>> = None

        fun toInputData(): Model.General.InputData =
            Model.General.InputData(name, batchInputShape, batchSize, dtype, sparse)

        companion object {
            operator fun invoke(
                name: String,
                shape: List<Int?>,
                batchSize: Int? = null,
                dtype: Number? = null,
                sparse: Boolean = false
            ) = InputLayer(name, shape, batchSize, dtype, sparse).untrainable()
        }
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/BatchNormalization
     *
     * Does not support the `adjustment` parameter.
     */
    data class BatchNormalization(
        override val name: String,
        override val inputs: Option<Set<String>>,
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
    ) : SealedLayer()

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Conv2D
     */
    data class Conv2D(
        override val name: String,
        override val inputs: Option<Set<String>>,
        val filters: Int,
        val kernel: Tuple2<Int, Int>,
        val activation: Activation
    ) : SealedLayer()

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Dense
     */
    data class Dense(
        override val name: String,
        override val inputs: Option<Set<String>>,
        val units: Int,
        val activation: Activation
    ) : SealedLayer()

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Dropout
     */
    data class Dropout(
        override val name: String,
        override val inputs: Option<Set<String>>,
        val rate: Double,
        val noiseShape: List<Int>? = null,
        val seed: Int? = null
    ) : SealedLayer() {

        init {
            require(rate in 0.0..1.0) {
                "rate ($rate) was outside the allowed range of [0, 1]."
            }
        }
    }

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/Flatten
     */
    data class Flatten(
        override val name: String,
        override val inputs: Option<Set<String>>,
        val dataFormat: DataFormat? = null
    ) : SealedLayer()

    /**
     * https://www.tensorflow.org/versions/r1.14/api_docs/python/tf/keras/layers/MaxPool2D
     */
    data class MaxPooling2D(
        override val name: String,
        override val inputs: Option<Set<String>>,
        val poolSize: Either<Int, Tuple2<Int, Int>> = Right(Tuple2(2, 2)),
        val strides: Either<Int, Tuple2<Int, Int>>? = null,
        val padding: PoolingPadding = PoolingPadding.Valid,
        val dataFormat: DataFormat? = null
    ) : SealedLayer()
}

/**
 * Creates a new [SealedLayer.MetaLayer.TrainableLayer].
 *
 * @receiver The [Layer] to wrap.
 * @param trainable Whether this layer should be trained.
 */
fun SealedLayer.trainable(trainable: Boolean = true) =
    SealedLayer.MetaLayer.TrainableLayer(name, this, trainable)

/**
 * Creates a new [SealedLayer.MetaLayer.UntrainableLayer].
 *
 * @receiver The [Layer] to wrap.
 */
fun SealedLayer.untrainable() =
    SealedLayer.MetaLayer.UntrainableLayer(name, this)
