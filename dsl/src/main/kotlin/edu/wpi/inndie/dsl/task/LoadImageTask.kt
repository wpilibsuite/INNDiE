package edu.wpi.inndie.dsl.task

import edu.wpi.inndie.dsl.Code
import edu.wpi.inndie.dsl.imports.makeImport
import edu.wpi.inndie.dsl.validator.path.PathValidator
import edu.wpi.inndie.dsl.variable.Variable
import edu.wpi.inndie.util.singleAssign
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Loads an image from a file.
 */
class LoadImageTask(name: String) : BaseTask(name), KoinComponent {

    /**
     * The file path to load this data from.
     */
    var imagePath: String by singleAssign()

    /**
     * The output the image will be stored in.
     */
    var imageOutput: Variable by singleAssign()

    /**
     * Validates the [imagePath].
     */
    private val pathValidator: PathValidator by inject()

    override val imports = setOf(makeImport("from PIL import Image"))

    override val inputs: Set<Variable> = emptySet()

    override val outputs: Set<Variable>
        get() = setOf(imageOutput)

    override val dependencies: MutableSet<Code<*>> = mutableSetOf()

    override fun isConfiguredCorrectly() = pathValidator.isValidPathName(imagePath) &&
        super.isConfiguredCorrectly()

    override fun code() = """
        |${imageOutput.name} = Image.open('$imagePath')
    """.trimMargin()
}
