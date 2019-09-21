package edu.wpi.axon.tfdata.layer

import arrow.core.Tuple2

/**
 * A sealed [Layer] implementation.
 */
sealed class SealedLayer : Layer {

    /**
     * A sealed [Layer] which delegates to another [layer].
     */
    sealed class MetaLayer(open val layer: Layer) : SealedLayer() {

        /**
         * A [Layer] which is trainable.
         *
         * @param trainable Whether this layer should be trained.
         */
        data class TrainableLayer(
            override val name: String,
            override val layer: Layer,
            val trainable: Boolean
        ) : MetaLayer(layer), Layer by layer {
            init {
                require(layer !is MetaLayer)
            }
        }

        /**
         * A [Layer] which is untrainable. This should not be confused with a [TrainableLayer]
         * where [TrainableLayer.trainable] is `true`. An [UntrainableLayer] is IMPOSSIBLE to train.
         */
        data class UntrainableLayer(
            override val name: String,
            override val layer: Layer
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
        override val name: String
    ) : SealedLayer()

    /**
     * A layer that accepts input data and has no parameters.
     */
    data class InputLayer
    private constructor(
        override val name: String,
        val batchInputShape: List<Int?>
    ) : SealedLayer() {
        companion object {
            operator fun invoke(name: String, shape: List<Int?>) =
                InputLayer(name, shape).untrainable()
        }
    }

    /**
     * A Dense layer.
     *
     * @param units The number of neurons.
     * @param activation The [Activation] function.
     */
    data class Dense(
        override val name: String,
        val units: Int,
        val activation: Activation
    ) : SealedLayer()

    /**
     * A 2D convolutional layer.
     *
     * @param filters The output dimension.
     * @param kernel The (x,y) size of the kernel.
     * @param activation The [Activation] function.
     */
    data class Conv2D(
        override val name: String,
        val filters: Int,
        val kernel: Tuple2<Int, Int>,
        val activation: Activation
    ) : SealedLayer()
}

/**
 * Creates a new [SealedLayer.MetaLayer.TrainableLayer].
 *
 * @receiver The [Layer] to wrap.
 * @param trainable Whether this layer should be trained.
 */
fun Layer.trainable(trainable: Boolean = true) =
    SealedLayer.MetaLayer.TrainableLayer(name, this, trainable)

/**
 * Creates a new [SealedLayer.MetaLayer.UntrainableLayer].
 *
 * @receiver The [Layer] to wrap.
 */
fun Layer.untrainable() =
    SealedLayer.MetaLayer.UntrainableLayer(name, this)
