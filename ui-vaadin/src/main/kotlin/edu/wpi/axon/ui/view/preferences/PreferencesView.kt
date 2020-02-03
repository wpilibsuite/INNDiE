package edu.wpi.axon.ui.view.preferences

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.VaadinDsl
import com.github.mvysny.karibudsl.v10.beanValidationBinder
import com.github.mvysny.karibudsl.v10.bind
import com.github.mvysny.karibudsl.v10.button
import com.github.mvysny.karibudsl.v10.comboBox
import com.github.mvysny.karibudsl.v10.h3
import com.github.mvysny.karibudsl.v10.hr
import com.github.mvysny.karibudsl.v10.init
import com.github.mvysny.karibudsl.v10.numberField
import com.github.mvysny.karibudsl.v10.onLeftClick
import com.github.mvysny.karibudsl.v10.toLong
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.upload.receivers.MemoryBuffer
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import edu.wpi.axon.aws.preferences.Preferences
import edu.wpi.axon.aws.preferences.PreferencesManager
import edu.wpi.axon.plugin.PluginManager
import edu.wpi.axon.ui.MainLayout
import edu.wpi.axon.ui.component.upload
import edu.wpi.axon.ui.view.HasNotifications
import edu.wpi.axon.util.datasetPluginManagerName
import edu.wpi.axon.util.testPluginManagerName
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named
import software.amazon.awssdk.services.ec2.model.InstanceType

@Route(layout = MainLayout::class)
@PageTitle("Preferences")
class PreferencesView : KComposite(), HasNotifications, KoinComponent {
    private val binder = beanValidationBinder<Preferences>()
    private val datasetPluginManager by inject<PluginManager>(named(datasetPluginManagerName))
    private val preferencesManager by inject<PreferencesManager>()
    private val testPluginManager by inject<PluginManager>(named(testPluginManagerName))

    init {
        ui {
            verticalLayout {
                setSizeFull()
                section("AWS") {
                    comboBox<InstanceType>("Training Instance Type") {
                        setItems(InstanceType.knownValues().stream().sorted())
                        isPreventInvalidInput = true
                        isRequired = true
                        bind(binder).asRequired().bind(Preferences::defaultEC2NodeType)
                    }
                    numberField("Status Polling Delay (ms)") {
                        width = "12em"
                        isPreventInvalidInput = true
                        bind(binder)
                                .asRequired()
                                .toLong()
                                .withValidator { value, _ ->
                                    if (value != null && value > 0) {
                                        ValidationResult.ok()
                                    } else {
                                        ValidationResult.error("must be greater than zero.")
                                    }
                                }.bind(Preferences::statusPollingDelay)
                    }
                }
                section("Plugins") {
                    upload(MemoryBuffer()) {
                        addSucceededListener {
                            showNotification("Uploaded File ${it.fileName}, ${it.mimeType}")
                        }
                        addFailedListener {
                            showNotification("Could not upload file: ${it.reason}")
                        }
                        setAcceptedFileTypes("text/x-python-script")
                    }
                }
                button("Save") {
                    onLeftClick {
                        val preferences = Preferences()
                        if (binder.validate().isOk && binder.writeBeanIfValid(preferences)) {
                            preferencesManager.put(preferences)
                            showNotification("Preferences Saved")
                        } else {
                            showNotification("Could not save preferences!")
                            LOGGER.warn { "Could not save invalid preferences:\n$preferences" }
                        }
                    }
                }
            }
        }

        binder.readBean(preferencesManager.get())
    }

    @VaadinDsl
    private fun (@VaadinDsl HasComponents).section(title: String? = null, block: (@VaadinDsl HasComponents).() -> Unit = {}) = init(VerticalLayout()) {
        if (title != null) h3(title)
        block()
        hr()
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
