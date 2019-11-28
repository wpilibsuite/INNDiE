@file:Suppress("UnstableApiUsage")

package edu.wpi.axon.tflayerloader

import arrow.Kind
import arrow.core.Either
import arrow.typeclasses.MonadError
import com.google.common.graph.Graph
import edu.wpi.axon.tfdata.layer.Layer
import edu.wpi.axon.util.allIn
import edu.wpi.axon.util.breadthFirstSearch

/**
 * Validated a layer graph.
 *
 * @param layerGraph The layer graph to validate.
 * @return No error if the graph is valid.
 */
fun <F> MonadError<F, String>.layerGraphIsValid(
    layerGraph: Graph<Layer.MetaLayer>
): Kind<F, Unit> {
    val nodeIterator = layerGraph.nodes().iterator()
    val nodesHaveDeclaredInputs = if (nodeIterator.hasNext()) {
        // Only check the nodes if there are any, otherwise calling next() will throw
        tailRecM(nodeIterator.next()) { layer ->
            hasInputs(layer).flatMap { inputsAreDeclared(layerGraph, layer) }.map {
                if (nodeIterator.hasNext()) {
                    Either.Left(nodeIterator.next())
                } else {
                    Either.Right(Unit)
                }
            }
        }
    } else {
        // No nodes means that they are all configured correctly
        just(Unit)
    }

    return layerNamesAreUnique(layerGraph).flatMap { nodesHaveDeclaredInputs }
}

/**
 * @param layerGraph The layer graph.
 * @return No error if all the layers' names are unique.
 */
private fun <F> MonadError<F, String>.layerNamesAreUnique(layerGraph: Graph<Layer.MetaLayer>) =
    layerGraph.nodes().let { nodes ->
        if (nodes.mapTo(mutableSetOf()) { it.name }.size == nodes.size) {
            just(Unit)
        } else {
            raiseError("Not all the layer names are unique: ${nodes.joinToString("\n")}")
        }
    }

/**
 * A layer must have inputs (unless it's a [Layer.InputLayer], which can't have inputs).
 *
 * @param layer The layer.
 * @return No error if the layer has inputs.
 */
private fun <F> MonadError<F, String>.hasInputs(layer: Layer.MetaLayer) =
    if (layer.inputs != null || layer.layer is Layer.InputLayer) {
        just(Unit)
    } else {
        raiseError("The layer does not have inputs: $layer")
    }

/**
 * A layer's inputs must already be in the layer graph by the time they are used.
 *
 * @param layerGraph The layer graph.
 * @param layer The layer to check.
 * @return No error if all inputs are already declared.
 */
private fun <F> MonadError<F, String>.inputsAreDeclared(
    layerGraph: Graph<Layer.MetaLayer>,
    layer: Layer.MetaLayer
): Kind<F, Unit> = when (val inputs = layer.inputs) {
    null -> just(Unit)
    else -> {
        val predecessorLayers =
            layerGraph.breadthFirstSearch(layer, Graph<Layer.MetaLayer>::predecessors)
                .map { it.name }

        if (inputs allIn predecessorLayers) {
            just(Unit)
        } else {
            raiseError(
                "Not all of the layer's inputs are declared previously: " +
                    (inputs subtract predecessorLayers).joinToString()
            )
        }
    }
}
