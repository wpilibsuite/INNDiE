package edu.wpi.axon.ui.controller

import edu.wpi.axon.plugin.Plugin
import edu.wpi.axon.plugin.PluginManager
import edu.wpi.axon.util.datasetPluginManagerName
import edu.wpi.axon.util.testPluginManagerName
import tornadofx.Controller
import tornadofx.SortedFilteredList

abstract class PluginStore : Controller() {
    protected abstract val pluginManager: PluginManager

    val plugins = SortedFilteredList<Plugin>()

    fun addPlugin(plugin: Plugin.Unofficial) {
        pluginManager.addUnofficialPlugin(plugin)
        plugins.add(plugin)
    }

    fun removePlugin(plugin: Plugin.Unofficial) {
        pluginManager.removeUnofficialPlugin(plugin.name)
        plugins.remove(plugin)
    }
}

class DatasetPluginStore : PluginStore() {
    override val pluginManager by di<PluginManager>(datasetPluginManagerName)

    init {
        plugins.items.setAll(pluginManager.listPlugins())
    }
}

class TestPluginStore : PluginStore() {
    override val pluginManager by di<PluginManager>(testPluginManagerName)

    init {
        plugins.items.setAll(pluginManager.listPlugins())
    }
}
