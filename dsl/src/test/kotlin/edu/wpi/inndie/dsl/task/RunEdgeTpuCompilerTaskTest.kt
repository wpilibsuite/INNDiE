package edu.wpi.inndie.dsl.task

import edu.wpi.inndie.dsl.alwaysValidImportValidator
import edu.wpi.inndie.dsl.alwaysValidPathValidator
import edu.wpi.inndie.dsl.mockVariableNameGenerator
import edu.wpi.inndie.testutil.KoinTestFixture
import io.kotlintest.matchers.booleans.shouldBeFalse
import io.kotlintest.matchers.booleans.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module

internal class RunEdgeTpuCompilerTaskTest : KoinTestFixture() {

    @Test
    fun `test output model filename`() {
        startKoin {
            modules(
                module {
                    alwaysValidImportValidator()
                    alwaysValidPathValidator()
                }
            )
        }

        RunEdgeTpuCompilerTask.getEdgeTpuCompiledModelFilename("input.tflite")
            .shouldBe("input_edgetpu.tflite")
    }

    @Test
    fun `test compiler log filename`() {
        startKoin {
            modules(
                module {
                    alwaysValidImportValidator()
                    alwaysValidPathValidator()
                }
            )
        }

        RunEdgeTpuCompilerTask.getEdgeTpuCompilerLogFilename("input.tflite")
            .shouldBe("input_edgetpu.log")
    }

    @Test
    fun `test with invalid input model filename`() {
        startKoin {
            modules(
                module {
                    alwaysValidImportValidator()
                    alwaysValidPathValidator()
                    mockVariableNameGenerator()
                }
            )
        }

        RunEdgeTpuCompilerTask("").apply {
            inputModelFilename = "input.invalid"
        }.isConfiguredCorrectly().shouldBeFalse()
    }

    @Test
    fun `test code gen`() {
        startKoin {
            modules(
                module {
                    alwaysValidImportValidator()
                    alwaysValidPathValidator()
                    mockVariableNameGenerator()
                }
            )
        }

        val task = RunEdgeTpuCompilerTask("").apply {
            inputModelFilename = "input.tflite"
            outputDir = "."
        }

        task.isConfiguredCorrectly().shouldBeTrue()
        task.code().shouldBe(
            """
            |subprocess.run(["edgetpu_compiler", "./input.tflite", "-o ."])
            |with open("./input_edgetpu.log", "r") as f:
            |    print(f.read())
            """.trimMargin()
        )
    }
}
