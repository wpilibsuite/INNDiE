package edu.wpi.axon.dsl

import edu.wpi.axon.dsl.imports.DefaultImportValidator
import edu.wpi.axon.dsl.imports.ImportValidator
import edu.wpi.axon.dsl.validator.path.DefaultPathValidator
import edu.wpi.axon.dsl.validator.path.PathValidator
import edu.wpi.axon.dsl.validator.variablename.PythonVariableNameValidator
import edu.wpi.axon.dsl.validator.variablename.VariableNameValidator
import edu.wpi.axon.tfdata.code.DatasetToCode
import edu.wpi.axon.tfdata.code.DefaultDatasetToCode
import edu.wpi.axon.tfdata.code.layer.LayerToCode
import edu.wpi.axon.tfdata.code.layer.SequentialLayerToCode
import edu.wpi.axon.tfdata.code.loss.DefaultLossToCode
import edu.wpi.axon.tfdata.code.loss.LossToCode
import edu.wpi.axon.tfdata.code.optimizer.DefaultOptimizerToCode
import edu.wpi.axon.tfdata.code.optimizer.OptimizerToCode
import org.koin.dsl.module

fun defaultModule() = module {
    single<VariableNameValidator> { PythonVariableNameValidator() }
    single<PathValidator> { DefaultPathValidator() }
    single<ImportValidator> { DefaultImportValidator() }
    single<UniqueVariableNameGenerator> { DefaultUniqueVariableNameGenerator() }

    // TODO: Change this once there is a non-Sequential version
    single<LayerToCode> { SequentialLayerToCode() }

    single<OptimizerToCode> { DefaultOptimizerToCode() }
    single<LossToCode> { DefaultLossToCode() }
    single<DatasetToCode> { DefaultDatasetToCode() }
}
