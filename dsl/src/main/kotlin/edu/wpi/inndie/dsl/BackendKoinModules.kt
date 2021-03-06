package edu.wpi.inndie.dsl

import edu.wpi.inndie.dsl.imports.DefaultImportValidator
import edu.wpi.inndie.dsl.imports.ImportValidator
import edu.wpi.inndie.dsl.validator.path.DefaultPathValidator
import edu.wpi.inndie.dsl.validator.path.PathValidator
import edu.wpi.inndie.dsl.validator.variablename.PythonVariableNameValidator
import edu.wpi.inndie.dsl.validator.variablename.VariableNameValidator
import edu.wpi.inndie.tfdata.code.DefaultExampleDatasetToCode
import edu.wpi.inndie.tfdata.code.ExampleDatasetToCode
import edu.wpi.inndie.tfdata.code.layer.DefaultLayerToCode
import edu.wpi.inndie.tfdata.code.layer.LayerToCode
import edu.wpi.inndie.tfdata.code.loss.DefaultLossToCode
import edu.wpi.inndie.tfdata.code.loss.LossToCode
import edu.wpi.inndie.tfdata.code.optimizer.DefaultOptimizerToCode
import edu.wpi.inndie.tfdata.code.optimizer.OptimizerToCode
import org.koin.dsl.module

fun defaultBackendModule() = module {
    single<VariableNameValidator> { PythonVariableNameValidator() }
    single<PathValidator> { DefaultPathValidator() }
    single<ImportValidator> { DefaultImportValidator() }
    single<UniqueVariableNameGenerator> { DefaultUniqueVariableNameGenerator() }
    single<LayerToCode> { DefaultLayerToCode() }
    single<OptimizerToCode> { DefaultOptimizerToCode() }
    single<LossToCode> { DefaultLossToCode() }
    single<ExampleDatasetToCode> { DefaultExampleDatasetToCode() }
}
