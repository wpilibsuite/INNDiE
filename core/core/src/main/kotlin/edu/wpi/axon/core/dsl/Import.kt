package edu.wpi.axon.core.dsl

/**
 * An import statement.
 */
sealed class Import {

    abstract fun code(): String

    data class ModuleOnly(val module: String) : Import() {

        override fun code(): String {
            return "import $module"
        }
    }

    data class ModuleAndIdentifier(val module: String, val identifier: String) : Import() {

        override fun code(): String {
            return "from $module import $identifier"
        }
    }

    data class ModuleAndName(val module: String, val name: String) : Import() {

        override fun code(): String {
            return "import $module as $name"
        }
    }

    data class FullImport(val module: String, val identifier: String, val name: String) : Import() {

        override fun code(): String {
            return "from $module import $identifier as $name"
        }
    }
}
