package edu.wpi.axon.ui.view

import edu.wpi.axon.db.data.ModelSource
import edu.wpi.axon.db.data.TrainingScriptProgress
import edu.wpi.axon.examplemodel.ExampleModelManager
import edu.wpi.axon.plugin.PluginManager
import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.optimizer.Optimizer
import edu.wpi.axon.ui.model.AdamDto
import edu.wpi.axon.ui.model.AdamModel
import edu.wpi.axon.ui.model.DatasetModel
import edu.wpi.axon.ui.model.DatasetType
import edu.wpi.axon.ui.model.JobModel
import edu.wpi.axon.ui.model.ModelSourceModel
import edu.wpi.axon.ui.model.ModelSourceType
import edu.wpi.axon.util.FilePath
import edu.wpi.axon.util.datasetPluginManagerName
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Parent
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.util.StringConverter
import tornadofx.Commit
import tornadofx.Fieldset
import tornadofx.Fragment
import tornadofx.ItemFragment
import tornadofx.ItemViewModel
import tornadofx.Scope
import tornadofx.action
import tornadofx.bind
import tornadofx.bindTo
import tornadofx.booleanBinding
import tornadofx.borderpane
import tornadofx.button
import tornadofx.buttonbar
import tornadofx.center
import tornadofx.checkbox
import tornadofx.chooseFile
import tornadofx.combobox
import tornadofx.enableWhen
import tornadofx.field
import tornadofx.fieldset
import tornadofx.filterInput
import tornadofx.find
import tornadofx.form
import tornadofx.hbox
import tornadofx.isInt
import tornadofx.label
import tornadofx.pane
import tornadofx.separator
import tornadofx.spinner
import tornadofx.toObservable
import tornadofx.validator
import tornadofx.vbox
import tornadofx.getValue
import tornadofx.integerBinding
import tornadofx.isDouble
import tornadofx.setValue
import tornadofx.textfield
import tornadofx.visibleWhen

class JobEditor : Fragment() {
    private val job by inject<JobModel>()

    override val root = borderpane {
        center {
            add<JobConfiguration>()
        }
        bottom = buttonbar {
            button("Revert") {
                enableWhen(job.dirty)
                setOnAction {
                    job.rollback()
                }
            }
            button("Save") {
                enableWhen(job.status.booleanBinding {
                    it == TrainingScriptProgress.NotStarted
                }.and(job.dirty))
                setOnAction {
                    job.commit()
                }
            }
        }
    }
}

class JobConfiguration : Fragment("Configuration") {
    private val job by inject<JobModel>()
    private val datasetPluginManager by di<PluginManager>(datasetPluginManagerName)

    override val root = form {
        hbox(20) {
            vbox(20) {
                fieldset("Dataset") {
                    add<DatasetPicker>()
                    field("Plugin") {
                        combobox(job.datasetPlugin) {
                            items = datasetPluginManager.listPlugins().toList().toObservable()
                            cellFormat {
                                text = it.name.toLowerCase().capitalize()
                            }
                        }
                    }
                }
                separator()
                fieldset("Model") {
                    add<ModelPicker>()
                }
            }
            vbox(20) {
                fieldset {
                    field("Epochs") {
                        spinner(1, amountToStepBy = 1, editable = true, property = job.userEpochs) {
                            editor.apply {
                                filterInput {
                                    it.controlNewText.isInt()
                                }
                            }

                            validator {
                                if (it == null) error("The epochs field is required.") else null
                            }
                        }
                    }
                }
                separator()
                fieldset("Optimizer") {
                    field("Type") {
                        combobox(job.optimizerType) {
                            items = Optimizer::class.sealedSubclasses.toObservable()
                            cellFormat {
                                text = it.simpleName ?: "UNKNOWN"
                            }
                        }
                    }
                    field("Edit") {
                        button {
                            action {
                                find<OptimizerFragment>().openModal(modality = Modality.WINDOW_MODAL)
                            }
                        }
                    }
                }
            }
        }
    }
}

class DatasetPicker : ItemFragment<Dataset>() {
    private val job by inject<JobModel>()
    private val dataset = DatasetModel().bindTo(this)

    override val root = vbox {
        field("Type") {
            combobox(dataset.type) {
                items = DatasetType.values().toList().toObservable()
                cellFormat {
                    text = it.name.toLowerCase().capitalize()
                }
            }
        }
        field("Selection") {
            contentMap(dataset.type) {
                item(DatasetType.EXAMPLE) {
                    combobox(job.userDataset) {
                        items =
                            Dataset.ExampleDataset::class.sealedSubclasses.map { it.objectInstance }
                                .toObservable()
                        cellFormat {
                            text = it.displayName
                        }
                    }
                }
                item(DatasetType.CUSTOM) {
                    vbox {
                        button {
                            setOnAction {
                                val file = chooseFile(
                                    "Pick",
                                    arrayOf(FileChooser.ExtensionFilter("Any", "*.*"))
                                )
                                file.firstOrNull()?.let {
                                    job.userDataset.value =
                                        Dataset.Custom(FilePath.Local(it.path), it.name)
                                }
                            }
                        }
                        label(job.userDataset, converter = object : StringConverter<Dataset>() {
                            override fun toString(obj: Dataset?) = obj?.displayName ?: ""
                            override fun fromString(string: String) = null
                        })
                    }
                }
            }
        }
    }

    init {
        itemProperty.bind(job.userDataset)
    }
}

class ModelPicker : ItemFragment<ModelSource>() {
    private val job by inject<JobModel>()
    private val modelSource = ModelSourceModel().bindTo(this)
    private val exampleModelManager by di<ExampleModelManager>()

    override val root = vbox {
        field("Source") {
            combobox(modelSource.type) {
                items = ModelSourceType.values().toList().toObservable()
                cellFormat {
                    text = it.name.toLowerCase().capitalize()
                }
            }
        }
        contentMap(modelSource.type) {
            item(ModelSourceType.EXAMPLE) {
                combobox(job.userOldModelPath) {
                    items = exampleModelManager.getAllExampleModels().unsafeRunSync().map {
                        ModelSource.FromExample(it)
                    }.toObservable()
                    cellFormat {
                        text = (it as? ModelSource.FromExample)?.exampleModel?.name ?: ""
                    }
                }
            }
            item(ModelSourceType.FILE) {
                vbox {
                    label(
                        job.userOldModelPath,
                        converter = object : StringConverter<ModelSource>() {
                            override fun toString(obj: ModelSource?) =
                                (obj as? ModelSource.FromFile)?.filePath?.toString() ?: ""

                            override fun fromString(string: String?) = null
                        })
                }
            }
            item(ModelSourceType.JOB) {
                vbox {
                    label("Job")
                }
            }
        }
    }

    init {
        itemProperty.bind(job.userOldModelPath)
    }
}

class OptimizerFragment : Fragment() {
    private val job by inject<JobModel>()
    lateinit var model: ItemViewModel<*>

    override val root = form {
        fieldset("Edit Optimizer") {
            println("Loaded with opt: ${job.userOptimizer.value}")
            when (val opt = job.userOptimizer.value) {
                is Optimizer.Adam -> createAdamFields(opt)

                else -> Unit
            }
        }

        button("Save") {
            action {
                model.commit {
                    close()
                }
            }
        }
    }

    private fun Fieldset.createAdamFields(opt: Optimizer.Adam) {
        @Suppress("UNCHECKED_CAST")
        val adamModel = AdamModel(job.userOptimizer as Property<Optimizer.Adam>).apply {
            item = AdamDto(opt)
        }
        model = adamModel
        field("Learning Rate") {
            textfield(adamModel.learningRate) {
                filterInput { it.controlNewText.isDouble() }
            }
        }
        field("Beta 1") {
            textfield(adamModel.beta1) {
                filterInput { it.controlNewText.isDouble() }
            }
        }
        field("Beta 2") {
            textfield(adamModel.beta2) {
                filterInput { it.controlNewText.isDouble() }
            }
        }
        field("Epsilon") {
            textfield(adamModel.epsilon) {
                filterInput { it.controlNewText.isDouble() }
            }
        }
        field("AMS Grad") {
            checkbox(property = adamModel.amsGrad)
        }
    }
}

@JvmName("textfieldDouble")
fun EventTarget.textfield(property: ObservableValue<Double>, op: TextField.() -> Unit = {}) =
    textfield().apply {
        bind(property)
        op(this)
    }
