@file:SuppressWarnings("LongMethod", "LargeClass", "TooManyFunctions")

package edu.wpi.inndie.dsl.task

import edu.wpi.inndie.dsl.configuredCorrectly
import edu.wpi.inndie.testutil.KoinTestFixture
import edu.wpi.inndie.tfdata.code.loss.LossToCode
import edu.wpi.inndie.tfdata.code.optimizer.OptimizerToCode
import edu.wpi.inndie.tfdata.loss.Loss
import edu.wpi.inndie.tfdata.optimizer.Optimizer
import io.kotlintest.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module

internal class CompileModelTaskTest : KoinTestFixture() {

    @Test
    fun `compile with normal optimizer, loss, and no metrics`() {
        val mockOptimizer = mockk<Optimizer>()
        val mockLoss = mockk<Loss>()

        val optimizerToCode = mockk<OptimizerToCode> {
            every { makeNewOptimizer(mockOptimizer) } returns "optimizer"
        }
        val lossToCode = mockk<LossToCode> {
            every { makeNewLoss(mockLoss) } returns "loss"
        }

        startKoin {
            modules(module {
                single { optimizerToCode }
                single { lossToCode }
            })
        }

        val task = CompileModelTask("task").apply {
            modelInput = configuredCorrectly("modelName")
            optimizer = mockOptimizer
            loss = mockLoss
        }

        task.code() shouldBe """
            |modelName.compile(
            |    optimizer=optimizer,
            |    loss=loss,
            |    metrics=[]
            |)
        """.trimMargin()

        verify { optimizerToCode.makeNewOptimizer(mockOptimizer) }
        verify { lossToCode.makeNewLoss(mockLoss) }
        confirmVerified(optimizerToCode, lossToCode)
    }

    @Test
    fun `compile with normal optimizer, loss, and two metrics`() {
        val mockOptimizer = mockk<Optimizer>()
        val mockLoss = mockk<Loss>()

        val optimizerToCode = mockk<OptimizerToCode> {
            every { makeNewOptimizer(mockOptimizer) } returns "optimizer"
        }
        val lossToCode = mockk<LossToCode> {
            every { makeNewLoss(mockLoss) } returns "loss"
        }

        startKoin {
            modules(module {
                single { optimizerToCode }
                single { lossToCode }
            })
        }

        val task = CompileModelTask("task").apply {
            modelInput = configuredCorrectly("modelName")
            optimizer = mockOptimizer
            loss = mockLoss
            metrics = setOf("accuracy", "precision")
        }

        task.code() shouldBe """
            |modelName.compile(
            |    optimizer=optimizer,
            |    loss=loss,
            |    metrics=["accuracy", "precision"]
            |)
        """.trimMargin()

        verify { optimizerToCode.makeNewOptimizer(mockOptimizer) }
        verify { lossToCode.makeNewLoss(mockLoss) }
        confirmVerified(optimizerToCode, lossToCode)
    }
}
