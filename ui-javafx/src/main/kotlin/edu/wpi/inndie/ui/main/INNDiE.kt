package edu.wpi.inndie.ui.main

import edu.wpi.inndie.dsl.defaultBackendModule
import edu.wpi.inndie.ui.view.Main
import kotlin.reflect.KClass
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX

class INNDiE : App(Main::class) {
    init {
        startKoin {
            modules(
                    listOf(
                            defaultBackendModule(),
                            defaultFrontendModule()
                    )
            )
        }

        FX.dicontainer = object : DIContainer, KoinComponent {
            override fun <T : Any> getInstance(type: KClass<T>): T =
                getKoin().get(clazz = type, qualifier = null, parameters = null)

            override fun <T : Any> getInstance(type: KClass<T>, name: String): T =
                getKoin().get(clazz = type, qualifier = named(name), parameters = null)
        }

        importStylesheet("/material.css")
    }

    companion object {
        fun main() {
            tornadofx.launch<INNDiE>()
        }
    }
}

/*
 * https://github.com/edvin/tornadofx/issues/982
 */
private fun Any.importStylesheet(path: String) {
    val css = javaClass.getResource(path)
    if (css == null) {
        throw IllegalArgumentException("Unable to find $path")
    } else {
        tornadofx.importStylesheet(css.toExternalForm()) // this one works
    }
}
