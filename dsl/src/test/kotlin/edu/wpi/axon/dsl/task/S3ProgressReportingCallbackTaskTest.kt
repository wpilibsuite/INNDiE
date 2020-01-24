package edu.wpi.axon.dsl.task

import edu.wpi.axon.dsl.TaskConfigurationTestFixture
import edu.wpi.axon.dsl.configuredCorrectly
import edu.wpi.axon.dsl.mockVariableNameGenerator
import edu.wpi.axon.testutil.KoinTestFixture
import edu.wpi.axon.util.axonBucketName
import io.kotlintest.shouldBe
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal class S3ProgressReportingCallbackTaskConfigurationTest :
    TaskConfigurationTestFixture<S3ProgressReportingCallbackTask>(
        {
            S3ProgressReportingCallbackTask("").apply {
                modelName = RandomStringUtils.randomAlphanumeric(10)
                datasetName = RandomStringUtils.randomAlphanumeric(10)
            }
        },
        listOf(
            S3ProgressReportingCallbackTask::output
        )
    )

internal class S3ProgressReportingCallbackTaskTest : KoinTestFixture() {

    @Test
    fun `test code gen`() {
        startKoin {
            modules(module {
                single(named(axonBucketName)) { "b" }
                mockVariableNameGenerator()
            })
        }

        val task = S3ProgressReportingCallbackTask("").apply {
            modelName = "m"
            datasetName = "d"
            output = configuredCorrectly("output")
        }

        task.code().shouldBe(
            """
            |class var1(tf.keras.callbacks.Callback):
            |    def on_epoch_end(self, epoch, logs=None):
            |        axon.client.impl_update_training_progress("m", "d",
            |                                                  str(epoch + 1), "b",
            |                                                  None)
            |
            |output = var1()
            """.trimMargin()
        )
    }
}
