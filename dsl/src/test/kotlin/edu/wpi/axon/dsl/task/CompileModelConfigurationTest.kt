package edu.wpi.axon.dsl.task

import edu.wpi.axon.dsl.TaskConfigurationTestFixture

internal class CompileModelConfigurationTest : TaskConfigurationTestFixture<CompileModelTask>(
    CompileModelTask::class,
    listOf(
        CompileModelTask::modelInput
    )
)
