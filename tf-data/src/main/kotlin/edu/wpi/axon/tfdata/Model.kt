package edu.wpi.axon.tfdata

import edu.wpi.axon.tfdata.layer.SealedLayer

sealed class Model {

    /**
     * A linear stack of layers.
     */
    data class Sequential(
        val name: String,
        val batchInputShape: List<Int?>,
        val layers: Set<SealedLayer.MetaLayer>
    ) : Model()
}
