package edu.wpi.inndie.dsl.task

import edu.wpi.inndie.dsl.Code
import edu.wpi.inndie.dsl.UniqueVariableNameGenerator
import edu.wpi.inndie.dsl.imports.Import
import edu.wpi.inndie.dsl.imports.makeImport
import edu.wpi.inndie.dsl.variable.Variable
import edu.wpi.inndie.tfdata.Dataset
import edu.wpi.inndie.tfdata.code.ExampleDatasetToCode
import edu.wpi.inndie.util.singleAssign
import org.koin.core.inject

/**
 * Loads an example dataset.
 */
class LoadExampleDatasetTask(name: String) : BaseTask(name) {

    /**
     * The dataset to load.
     */
    var dataset: Dataset.ExampleDataset by singleAssign()

    /**
     * The input data for training.
     */
    var xTrainOutput: Variable by singleAssign()

    /**
     * The target data for training.
     */
    var yTrainOutput: Variable by singleAssign()

    /**
     * The input data for validation.
     */
    var xTestOutput: Variable by singleAssign()

    /**
     * The target data for validation.
     */
    var yTestOutput: Variable by singleAssign()

    private val exampleDatasetToCode: ExampleDatasetToCode by inject()

    override val imports: Set<Import> = setOf(
        makeImport("import tensorflow as tf")
    )

    override val inputs: Set<Variable> = setOf()

    override val outputs: Set<Variable>
        get() = setOf(xTrainOutput, yTrainOutput, xTestOutput, yTestOutput)

    override val dependencies: MutableSet<Code<*>> = mutableSetOf()

    private val variableNameGenerator: UniqueVariableNameGenerator by inject()

    override fun code(): String {
        val methodName = variableNameGenerator.uniqueVariableName()
        val output = "(${xTrainOutput.name}, ${yTrainOutput.name}), " +
            "(${xTestOutput.name}, ${yTestOutput.name})"
        return """
            |def $methodName():
            |${exampleDatasetToCode.datasetToCode(dataset)}
            |
            |$output = $methodName()
        """.trimMargin()
    }
}
