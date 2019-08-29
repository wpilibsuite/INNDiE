package edu.wpi.axon.dsl

import arrow.data.ListK
import arrow.data.extensions.listk.applicative.applicative
import arrow.data.fix
import arrow.data.k
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import com.google.common.graph.MutableGraph
import edu.wpi.axon.dsl.container.PolymorphicNamedDomainObjectContainer

/**
 * Parses a [PolymorphicNamedDomainObjectContainer] of [Code] into an [ImmutableGraph], assuring
 * that all [Code] nodes are unique and dependencies are properly ordered. Nodes on separate
 * branches of the graph will not be deduplicated to preserve those dependencies. For example,
 * this is a possible output graph:
 * +---+      +---+
 * | A +----->+ B +--------+
 * +---+      +---+        v
 *                       +-+-+
 *                       | D |
 *                       +-+-+
 * +---+      +---+        ^
 * | A +----->+ C +--------+
 * +---+      +---+
 */
@Suppress("UnstableApiUsage")
class CodeGraph(
    private val container: PolymorphicNamedDomainObjectContainer<AnyCode>
) {

    val graph: ImmutableGraph<AnyCode> by lazy {
        val mutableGraph = GraphBuilder.directed()
            .allowsSelfLoops(false)
            .expectedNodeCount(container.size)
            .build<AnyCode>()

        container.forEach { (_, code) ->
            populateGraphUsingDependencies(mutableGraph, code)
        }

        populateGraphUsingVariables(mutableGraph, container)

        // TODO: Verify there are no islands in the final graph

        ImmutableGraph.copyOf(mutableGraph)
    }

    /**
     * Add nodes for codes and edges between codes that are linked with [Code.dependencies].
     */
    private fun populateGraphUsingDependencies(
        graph: MutableGraph<AnyCode>,
        code: AnyCode
    ) {
        // Make sure nodes without dependencies are in the graph
        graph.addNode(code)

        code.dependencies.forEach {
            // Don't recurse if the edge was already in the graph
            if (graph.putEdge(it, code)) {
                require(!hasCircuits(graph, code)) {
                    "Adding an edge from $it to $code caused a circuit."
                }

                it.dependencies.forEach {
                    populateGraphUsingDependencies(graph, it)
                }
            }
        }
    }

    /**
     * Add edges between pairs of codes where one code's [Code.outputs] is the other code's
     * [Code.inputs].
     */
    private fun populateGraphUsingVariables(
        graph: MutableGraph<AnyCode>,
        container: PolymorphicNamedDomainObjectContainer<AnyCode>
    ) {
        val codesK = container.values.toList().k()
        ListK.applicative()
            .tupled(codesK, codesK)
            .fix()
            .filter { (taskA, taskB) ->
                taskA.outputs anyIn taskB.inputs
            }
            .forEach { (codeWithOutputs, codeWithInputs) ->
                graph.putEdge(codeWithOutputs, codeWithInputs)
                require(!hasCircuits(graph, codeWithInputs)) {
                    "Adding an edge between $codeWithOutputs and $codeWithInputs caused a circuit."
                }
            }
    }

    private fun hasCircuits(
        graph: MutableGraph<AnyCode>,
        root: AnyCode,
        visited: Set<AnyCode> = emptySet()
    ): Boolean {
        graph.successors(root).forEach { node ->
            val reachable = bfs(graph, node)

            // Finding an already visited node in the new reachable set implies a circuit. The
            // reachable set will strictly decrease for successor nodes in a DAG.
            if (visited anyIn reachable || hasCircuits(graph, node, visited + node)) {
                return true
            }
        }

        return false
    }

    private fun bfs(graph: MutableGraph<AnyCode>, root: AnyCode): Set<AnyCode> {
        val visited = mutableSetOf<AnyCode>()
        val queue = mutableListOf<AnyCode>()

        visited += root
        queue += root

        while (queue.isNotEmpty()) {
            val node = queue.first()
            queue.remove(node)

            graph.successors(node).forEach {
                if (it !in visited) {
                    visited += it
                    queue += it
                }
            }
        }

        return visited
    }
}
