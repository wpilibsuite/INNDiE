package edu.wpi.axon.tfdata.code

import edu.wpi.axon.tfdata.optimizer.Optimizer
import io.kotlintest.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class DefaultOptimizerToCodeTest {

    private val optimizerToCode = DefaultOptimizerToCode()

    @ParameterizedTest
    @MethodSource("optimizerSource")
    fun `test optimizers`(optimizer: Optimizer, expected: String) {
        optimizerToCode.makeNewOptimizer(optimizer) shouldBe expected
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun optimizerSource() = listOf(
            Arguments.of(
                Optimizer.Adam(0.001, 0.9, 0.999, 1e-7, false),
                """tf.keras.optimizers.Adam(0.001, 0.9, 0.999, 1.0E-7, False)"""
            )
        )
    }
}
