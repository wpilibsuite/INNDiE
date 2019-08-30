package edu.wpi.axon.dsl

import arrow.data.Valid
import edu.wpi.axon.dsl.imports.ImportValidator
import io.mockk.every
import io.mockk.mockk
import org.koin.core.module.Module

internal fun Module.alwaysValidImportValidator() {
    single<ImportValidator> {
        mockk { every { validateImports(any()) } returns Valid(emptySet()) }
    }
}