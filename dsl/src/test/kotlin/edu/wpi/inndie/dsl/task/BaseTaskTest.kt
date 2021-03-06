package edu.wpi.inndie.dsl.task

import arrow.core.Invalid
import arrow.core.Nel
import edu.wpi.inndie.dsl.Code
import edu.wpi.inndie.dsl.alwaysValidImportValidator
import edu.wpi.inndie.dsl.configuredIncorrectly
import edu.wpi.inndie.dsl.imports.Import
import edu.wpi.inndie.dsl.imports.ImportValidator
import edu.wpi.inndie.dsl.variable.Variable
import edu.wpi.inndie.testutil.KoinTestFixture
import io.kotlintest.matchers.booleans.shouldBeFalse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module

internal class BaseTaskTest : KoinTestFixture() {

    @Test
    fun `an invalid input means the task is invalid`() {
        startKoin {
            modules(module {
                alwaysValidImportValidator()
            })
        }

        val task = stubTask(inputs = setOf(configuredIncorrectly()))
        task.isConfiguredCorrectly().shouldBeFalse()
    }

    @Test
    fun `an invalid output means the task is invalid`() {
        startKoin {
            modules(module {
                alwaysValidImportValidator()
            })
        }

        val task = stubTask(outputs = setOf(configuredIncorrectly()))
        task.isConfiguredCorrectly().shouldBeFalse()
    }

    @Test
    fun `an invalid import means the task is invalid`() {
        val imports = setOf(Import.ModuleOnly(""))

        val mockImportValidator = mockk<ImportValidator> {
            every { validateImports(imports) } returns
                Invalid(Nel.fromListUnsafe(imports.toList()))
        }

        startKoin {
            modules(module {
                single { mockImportValidator }
            })
        }

        val task = stubTask(imports = imports)
        task.isConfiguredCorrectly().shouldBeFalse()
    }

    @SuppressWarnings("LongParameterList")
    private fun stubTask(
        name: String = "task1",
        imports: Set<Import> = setOf(),
        inputs: Set<Variable> = setOf(),
        outputs: Set<Variable> = setOf(),
        dependencies: MutableSet<Code<*>> = mutableSetOf(),
        code: String = ""
    ): Task = object : BaseTask(name) {
        override val imports: Set<Import> = imports
        override val inputs: Set<Variable> = inputs
        override val outputs: Set<Variable> = outputs
        override val dependencies: MutableSet<Code<*>> = dependencies
        override fun code() = code
    }
}
