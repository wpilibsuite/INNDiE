package edu.wpi.inndie.dsl.variable

import edu.wpi.inndie.dsl.Configurable
import edu.wpi.inndie.dsl.validator.variablename.VariableNameValidator
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * A [Variable] is literally a variable in the generated code.
 *
 * @param name The name of this [Variable] (this will become the name in the code). This name is
 * also used to assure variable uniqueness.
 */
open class Variable(val name: String) : Configurable, KoinComponent {

    /**
     * Validates the variable name.
     */
    private val variableNameValidator: VariableNameValidator by inject()

    override fun isConfiguredCorrectly() = variableNameValidator.isValidVariableName(name)

    override fun toString() = "Variable(name=$name)"
}
