package edu.wpi.axon.core.dsl

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import edu.wpi.axon.core.dsl.container.DefaultPolymorphicNamedDomainObjectContainer
import edu.wpi.axon.core.dsl.task.InferenceTask
import edu.wpi.axon.core.dsl.task.YoloV3PostprocessTask
import edu.wpi.axon.core.dsl.validator.path.DefaultPathValidator
import edu.wpi.axon.core.dsl.validator.path.PathValidator
import edu.wpi.axon.core.dsl.validator.variablename.PythonVariableNameValidator
import edu.wpi.axon.core.dsl.validator.variablename.VariableNameValidator
import edu.wpi.axon.core.dsl.variable.ClassLabels
import edu.wpi.axon.core.dsl.variable.ImageInputData
import edu.wpi.axon.core.dsl.variable.InferenceSession
import edu.wpi.axon.core.dsl.variable.Variable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.util.concurrent.CountDownLatch

@Suppress("UNUSED_VARIABLE")
internal class ScriptGeneratorDslIntegrationTest : KoinTest {

    @AfterEach
    fun afterEach() {
        stopKoin()
    }

    @SuppressWarnings("LongMethod", "MaxLineLength")
    @Test
    fun `run yolov3 model`() {
        startKoin {
            modules(module {
                single<VariableNameValidator> { PythonVariableNameValidator() }
                single<PathValidator> { DefaultPathValidator() }
            })
        }

        val dsl = ScriptGeneratorDsl(DefaultPolymorphicNamedDomainObjectContainer.of(), DefaultPolymorphicNamedDomainObjectContainer.of()) {
            val session by variables.creating(InferenceSession::class) {
                path = "yolov3.onnx"
            }

            val classes by variables.creating(ClassLabels::class) {
                path = "coco_classes.txt"
            }

            val inputData by variables.creating(ImageInputData::class) {
                path = "horses.jpg"
            }

            val inferenceOutput by variables.creating(Variable::class)
            val inferenceTask by tasks.running(InferenceTask::class) {
                input = inputData
                inferenceSession = session
                output = inferenceOutput
            }

            val postProcessedOutput by variables.creating(Variable::class)
            val postProcessTask by tasks.running(YoloV3PostprocessTask::class) {
                input = inferenceOutput
                output = postProcessedOutput
            }

            scriptOutput = postProcessedOutput
        }

        assertThat(
            dsl.computeImports(), equalTo(
                setOf(
                    Import.ModuleOnly("onnxruntime"),
                    Import.ModuleAndIdentifier("PIL", "Image"),
                    Import.ModuleOnly("onnx"),
                    Import.ModuleAndIdentifier("axon", "postprocessYolov3"),
                    Import.ModuleAndName("numpy", "np")
                )
            )
        )

        val code = dsl.code()
        assertEquals(
            """
            |from PIL import Image
            |from axon import postprocessYolov3
            |import numpy as np
            |import onnx
            |import onnxruntime
            |
            |inputData = Image.open('horses.jpg')
            |imageData = preprocess(inputData)
            |imageSize = np.array([inputData.size[1], inputData.size[0]], dtype=np.float32).reshape(1, 2)
            |
            |session = onnxruntime.InferenceSession('yolov3.onnx')
            |
            |sessionInputNames = session.get_inputs()
            |inferenceOutput = session.run(None, {sessionInputNames[0].name: imageData, sessionInputNames[1].name: imageSize})
            |
            |postProcessedOutput = postprocessYolov3(inferenceOutput)
            """.trimMargin(),
            code
        )
    }

    @Test
    fun `code dependencies should be called`() {
        startKoin {
            modules(module {
                single<VariableNameValidator> { PythonVariableNameValidator() }
                single<PathValidator> { DefaultPathValidator() }
            })
        }

        val codeLatch = CountDownLatch(2)
        ScriptGeneratorDsl(DefaultPolymorphicNamedDomainObjectContainer.of(), DefaultPolymorphicNamedDomainObjectContainer.of()) {
            val task1 by tasks.running(MockTask::class) {
                latch = codeLatch
            }

            val task2 by tasks.running(MockTask::class) {
                latch = codeLatch
            }
        }.code()

        assertThat(codeLatch.count, equalTo(0L))
    }

    @Test
    fun `recursive code dependencies should not cause task code gen to happen multiple times`() {
        startKoin {
            modules(module {
                single<VariableNameValidator> { PythonVariableNameValidator() }
                single<PathValidator> { DefaultPathValidator() }
            })
        }

        val codeLatch = CountDownLatch(3)
        ScriptGeneratorDsl(DefaultPolymorphicNamedDomainObjectContainer.of(), DefaultPolymorphicNamedDomainObjectContainer.of()) {
            val task1 by tasks.running(MockTask::class) {
                latch = codeLatch
            }

            val task2 by tasks.running(MockTask::class) {
                latch = codeLatch
                dependencies += task1
            }
        }.code()

        assertThat(codeLatch.count, equalTo(1L))
    }
}
