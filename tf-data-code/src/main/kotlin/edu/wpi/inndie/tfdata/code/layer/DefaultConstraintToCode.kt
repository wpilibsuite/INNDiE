package edu.wpi.inndie.tfdata.code.layer

import arrow.core.right
import edu.wpi.inndie.tfdata.code.namedArguments
import edu.wpi.inndie.tfdata.layer.Constraint

class DefaultConstraintToCode : ConstraintToCode {

    override fun makeNewConstraint(constraint: Constraint) = when (constraint) {
        is Constraint.MaxNorm -> makeNewConstraint(
            "MaxNorm",
            listOf(
                "max_value" to constraint.maxValue,
                "axis" to constraint.axis
            )
        ).right()

        is Constraint.MinMaxNorm -> makeNewConstraint(
            "MinMaxNorm",
            listOf(
                "min_value" to constraint.minValue,
                "max_value" to constraint.maxValue,
                "rate" to constraint.rate,
                "axis" to constraint.axis
            )
        ).right()

        is Constraint.NonNeg -> makeNewConstraint(
            "NonNeg",
            listOf()
        ).right()

        is Constraint.UnitNorm -> makeNewConstraint(
            "UnitNorm",
            listOf(
                "axis" to constraint.axis
            )
        ).right()
    }

    private fun makeNewConstraint(
        className: String,
        namedArgs: List<Pair<String, Number>>
    ) = """tf.keras.constraints.$className(${namedArguments(namedArgs)})"""
}
