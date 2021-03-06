package edu.wpi.inndie.dsl.task

import arrow.core.Valid
import edu.wpi.inndie.dsl.imports.ImportValidator
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Holds the common implementation details for Tasks.
 */
abstract class BaseTask(override val name: String) : Task, KoinComponent {

    private val importValidator: ImportValidator by inject()

    override fun isConfiguredCorrectly() = importValidator.validateImports(imports) is Valid &&
        inputs.all { it.isConfiguredCorrectly() } && outputs.all { it.isConfiguredCorrectly() }

    override fun toString() = "Task(name='$name', class='${this::class.simpleName}')"
}
