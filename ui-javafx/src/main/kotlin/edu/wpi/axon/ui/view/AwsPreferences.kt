package edu.wpi.axon.ui.view

import edu.wpi.axon.aws.preferences.Preferences
import edu.wpi.axon.ui.model.AwsPreferencesModel
import javafx.scene.control.ButtonBar
import software.amazon.awssdk.services.ec2.model.InstanceType
import tornadofx.View
import tornadofx.action
import tornadofx.button
import tornadofx.buttonbar
import tornadofx.combobox
import tornadofx.enableWhen
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.form
import tornadofx.isLong
import tornadofx.observable
import tornadofx.required
import tornadofx.textfield
import tornadofx.toObservable

class AwsPreferences: View("AWS Preferences") {
    val model by inject<AwsPreferencesModel>()

    override val root = form {
        fieldset("AWS") {
            field("Default Training Instance Type") {
                combobox(model.defaultEC2NodeTypeProperty) {
                    items = InstanceType.knownValues().toList().sorted().toObservable()
                    required()
                }
            }
            field("Polling Rate (ms)") {
                textfield(model.statusPollingDelayProperty) {
                    filterInput { it.controlNewText.isLong() && it.controlNewText.toLong() > 0 }
                    required()
                }
            }
        }
        buttonbar(ButtonBar.BUTTON_ORDER_NONE) {
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action {
                    model.rollback()
                    close()
                }
            }
            button("Apply", ButtonBar.ButtonData.APPLY) {
                enableWhen(model.dirty.and(model.valid))
                action {
                    model.commit()
                }
            }
            button("Ok", ButtonBar.ButtonData.OK_DONE) {
                enableWhen(model.valid)
                action {
                    model.commit()
                    close()
                }
            }
        }
    }
}