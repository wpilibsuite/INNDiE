package edu.wpi.axon.dsl.task

import edu.wpi.axon.dsl.alwaysInvalidPathValidator
import edu.wpi.axon.dsl.alwaysValidImportValidator
import edu.wpi.axon.dsl.alwaysValidPathValidator
import edu.wpi.axon.dsl.configuredCorrectly
import edu.wpi.inndie.testutil.KoinTestFixture
import io.kotlintest.matchers.booleans.shouldBeFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module

internal class LoadClassLabelsTest : KoinTestFixture() {

    @Test
    fun `test code`() {
        startKoin {
            modules(module {
                alwaysValidImportValidator()
                alwaysValidPathValidator()
            })
        }

        val task = LoadClassLabels("task").apply {
            classLabelsPath = "path1"
            classOutput = configuredCorrectly("var1")
        }

        assertEquals("var1 = [line.rstrip('\\n') for line in open('path1')]", task.code())
    }

    @Test
    fun `test invalid path`() {
        startKoin {
            modules(module {
                alwaysValidImportValidator()
                alwaysInvalidPathValidator()
            })
        }

        val task = LoadClassLabels("task").apply {
            classLabelsPath = "path1"
            classOutput = configuredCorrectly("var1")
        }

        task.isConfiguredCorrectly().shouldBeFalse()
    }
}
