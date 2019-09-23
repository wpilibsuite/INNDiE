package edu.wpi.axon.ui.model

import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.layer.SealedLayer
import edu.wpi.axon.tfdata.loss.Loss
import edu.wpi.axon.tfdata.optimizer.Optimizer
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import kotlin.reflect.KClass

data class TrainingModel (
        var userModelPath: String? = "",
        @NotNull
        var userDataset: KClass<out Dataset>? = null,
        var userOptimizer: KClass<out Optimizer>? = null,
        var userLoss: KClass<out Loss>? = null,
        var userMetrics: Set<String> = setOf(),
        @Min(5)
        var userEpochs: Double = 0.0,
        var userNewLayers: Set<SealedLayer.MetaLayer> = setOf(),
        var generateDebugComments: Boolean = false
)