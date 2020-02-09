package edu.wpi.axon.ui

import edu.wpi.axon.db.JobDb
import edu.wpi.axon.db.data.Job
import edu.wpi.axon.db.data.ModelSource
import edu.wpi.axon.db.data.TrainingScriptProgress
import edu.wpi.axon.examplemodel.ExampleModel
import edu.wpi.axon.examplemodel.ExampleModelManager
import edu.wpi.axon.plugin.Plugin
import edu.wpi.axon.plugin.PluginManager
import edu.wpi.axon.tfdata.Dataset
import edu.wpi.axon.tfdata.optimizer.Optimizer
import edu.wpi.axon.util.datasetPluginManagerName
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Callback
import javafx.util.converter.NumberStringConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named
import java.text.NumberFormat
import kotlin.reflect.KClass

enum class ModelChoice {
    Example, Custom, Job
}

class JobDetailView(
    job: Job,
    onClose: () -> Unit
) : AnchorPane(), KoinComponent {

    private val jobDb by inject<JobDb>()
    private val exampleModelManager by inject<ExampleModelManager>()
    private val pluginManager by inject<PluginManager>(named(datasetPluginManagerName))
    private val scope = CoroutineScope(Dispatchers.Default)
    private val jobProperty = SimpleObjectProperty<Job>()
    private val jobInProgressProperty = SimpleBooleanProperty()
    private val exampleModels = exampleModelManager.getAllExampleModels().unsafeRunSync()

    init {
        maxWidth = Double.MAX_VALUE
        prefWidth = 300.0

        jobProperty.addListener { _, _, newValue ->
            jobInProgressProperty.value = (newValue.status != TrainingScriptProgress.NotStarted)
        }

        jobProperty.value = job

        children.add(TabPane().apply {
            maxWidth = Double.MAX_VALUE
            setTopAnchor(this, 0.0)
            setRightAnchor(this, 0.0)
            setBottomAnchor(this, 0.0)
            setLeftAnchor(this, 0.0)

            tabs.add(createBasicTab(job, onClose))
            tabs.add(createInputTab(job))
            tabs.add(createTrainingTab(job))
            tabs.add(createOutputTab(job))
        })
    }

    private fun createBasicTab(job: Job, onClose: () -> Unit) = Tab().apply {
        isClosable = false
        text = "Basic"
        content = VBox().apply {
            spacing = 10.0
            padding = Insets(5.0)

            children.add(HBox().apply {
                maxWidth = Double.MAX_VALUE
                alignment = Pos.CENTER
                children.add(Label(job.name).apply {
                    font = Font.font(Font.getDefault().family, FontWeight.BOLD, 15.0)
                })
            })

            children.add(Button("Run").apply {
                disableProperty().bind(jobInProgressProperty)
                setOnAction {
                    scope.launch {
                        // TODO: Actually run the Job
                        jobProperty.value = jobDb.update(
                            jobProperty.value.id,
                            status = TrainingScriptProgress.Initializing
                        )
                    }
                }
            })

            children.add(Button("Close").apply {
                setOnAction { onClose() }
            })
        }
    }

    private fun createInputTab(job: Job) = Tab().apply {
        isClosable = false
        text = "Input"
        content = VBox().apply {
            spacing = 10.0
            padding = Insets(5.0)

            createLabeledField("Dataset") {
                makeValidatedNode(
                    ComboBox<Dataset>(),
                    {
                        if (it.selectionModel.selectedItem == null) {
                            ValidationResult.Error("Must select a dataset.")
                        } else {
                            ValidationResult.Success
                        }
                    },
                    {
                        disableProperty().bind(jobInProgressProperty)
                        cellFactory = Callback { DatasetCell() }
                        buttonCell = DatasetCell()
                        items.setAll(Dataset.ExampleDataset::class.sealedSubclasses.mapNotNull {
                            it.objectInstance
                        })
                        selectionModel.select(job.userDataset)
                        selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                            if (it.isValid) {
                                scope.launch {
                                    jobProperty.value = jobDb.update(
                                        jobProperty.value.id,
                                        userDataset = newValue
                                    )
                                }
                            }
                        }
                    }
                )
            }

            createLabeledField("Dataset Plugin") {
                makeValidatedNode(
                    ComboBox<Plugin>(),
                    {
                        if (it.selectionModel.selectedItem == null)
                            ValidationResult.Error("Must select a dataset plugin.")
                        else ValidationResult.Success
                    },
                    {
                        disableProperty().bind(jobInProgressProperty)
                        cellFactory = Callback { PluginCell() }
                        buttonCell = PluginCell()
                        val plugins = pluginManager.listPlugins()
                        items.setAll(plugins)
                        selectionModel.select(job.datasetPlugin)
                        selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                            if (it.isValid) {
                                scope.launch {
                                    jobProperty.value = jobDb.update(
                                        jobProperty.value.id,
                                        datasetPlugin = newValue
                                    )
                                }
                            }
                        }
                    }
                )
            }

            children.add(VBox().apply {
                spacing = 10.0
                alignment = Pos.TOP_LEFT

                val exampleModelForm: ValidatedNode<out Node>
                val customModelForm: ValidatedNode<out Node>
                val jobModelForm: ValidatedNode<out Node>

                val modelDetails = StackPane().apply {
                    alignment = Pos.TOP_LEFT
                    exampleModelForm = makeValidatedNode(
                        ComboBox<ExampleModel>(),
                        {
                            if (it.isVisible && it.selectionModel.selectedItem == null)
                                ValidationResult.Error("Must select a model.")
                            else ValidationResult.Success
                        },
                        {
                            disableProperty().bind(jobInProgressProperty)
                            cellFactory = Callback { ExampleModelCell() }
                            buttonCell = ExampleModelCell()
                            items.setAll(exampleModels)
                            when (val modelSource = job.userOldModelPath) {
                                is ModelSource.FromExample ->
                                    selectionModel.select(modelSource.exampleModel)
                                else -> selectionModel.select(exampleModels.first())
                            }
                            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                                if (it.isValid) {
                                    scope.launch {
                                        jobProperty.value = jobDb.update(
                                            jobProperty.value.id,
                                            userOldModelPath = ModelSource.FromExample(newValue)
                                        )
                                    }
                                }
                            }
                        }
                    )
                    children.add(exampleModelForm)

                    customModelForm = makeValidatedNode(
                        ComboBox<String>(),
                        {
                            if (it.isVisible && it.selectionModel.selectedItem == null)
                                ValidationResult.Error("Must select a model.")
                            else ValidationResult.Success
                        },
                        {
                            disableProperty().bind(jobInProgressProperty)
                            isVisible = false
                        }
                    )
                    children.add(customModelForm)

                    jobModelForm = makeValidatedNode(
                        ComboBox<String>(),
                        {
                            if (it.isVisible && it.selectionModel.selectedItem == null)
                                ValidationResult.Error("Must select a model.")
                            else ValidationResult.Success
                        },
                        {
                            disableProperty().bind(jobInProgressProperty)
                            isVisible = false
                        }
                    )
                    children.add(jobModelForm)
                }

                val modelComboBox = HBox().apply {
                    spacing = 5.0
                    alignment = Pos.CENTER_LEFT

                    children.add(Label("Model"))
                    val modelSelectionBox = ValidatedNode(
                        ComboBox<ModelChoice>().apply {
                            disableProperty().bind(jobInProgressProperty)
                            items.setAll(ModelChoice.values().toList())
                            selectionModel.select(ModelChoice.Example)
                            when (job.userOldModelPath) {
                                is ModelSource.FromExample ->
                                    selectionModel.select(ModelChoice.Example)
                                is ModelSource.FromFile -> selectionModel.select(ModelChoice.Custom)
                                is ModelSource.FromJob -> selectionModel.select(ModelChoice.Job)
                            }
                            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                                exampleModelForm.isVisible = false
                                customModelForm.isVisible = false
                                jobModelForm.isVisible = false
                                when (newValue) {
                                    ModelChoice.Example, null -> exampleModelForm.isVisible = true
                                    ModelChoice.Custom -> customModelForm.isVisible = true
                                    ModelChoice.Job -> jobModelForm.isVisible = true
                                }
                            }
                        },
                        {
                            if (it.selectionModel.selectedItem == null)
                                ValidationResult.Error("Must select a model.")
                            else ValidationResult.Success
                        },
                        setOf(exampleModelForm, customModelForm, jobModelForm)
                    )
                    children.add(modelSelectionBox)
                }

                children.add(modelComboBox)
                children.add(modelDetails)
            })
        }
    }

    private fun createTrainingTab(job: Job) = Tab().apply {
        isClosable = false
        text = "Training"
        content = VBox().apply {
            spacing = 10.0
            padding = Insets(5.0)

            createLabeledField("Epochs") {
                makeValidatedNode(
                    TextField(),
                    {
                        try {
                            val value = it.text.toInt()
                            if (value < 1) {
                                ValidationResult.Error("Must be at least 1.")
                            } else {
                                ValidationResult.Success
                            }
                        } catch (ex: NumberFormatException) {
                            ValidationResult.Error("Must be an integer.")
                        }
                    },
                    {
                        disableProperty().bind(jobInProgressProperty)
                        text = job.userEpochs.toString()
                        textProperty().addListener { _, _, newValue ->
                            if (it.isValid) {
                                scope.launch {
                                    jobProperty.value = jobDb.update(
                                        jobProperty.value.id,
                                        userEpochs = newValue.toInt()
                                    )
                                }
                            }
                        }
                    }
                )
            }

            createLabeledField("Optimizer") {
                VBox().apply {
                    spacing = 10.0
                    alignment = Pos.TOP_LEFT

                    lateinit var makeOptimizer: () -> Optimizer
                    val selector = makeValidatedNode(
                        ComboBox<KClass<out Optimizer>>(),
                        {
                            if (it.selectionModel.selectedItem == null)
                                ValidationResult.Error("Must select an optimizer.")
                            else ValidationResult.Success
                        },
                        {
                            disableProperty().bind(jobInProgressProperty)
                            cellFactory = Callback { OptimizerCell() }
                            buttonCell = OptimizerCell()
                            items.setAll(Optimizer::class.sealedSubclasses)
                            selectionModel.select(job.userOptimizer::class)
                            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                                if (it.isValid) {
                                    scope.launch {
                                        jobProperty.value = jobDb.update(
                                            jobProperty.value.id,
                                            userOptimizer = makeOptimizer()
                                        )
                                    }
                                }
                            }
                        }
                    )
                    children.add(selector)

                    children.add(StackPane().apply {
                        children.add(VBox().apply {
                            val learningRateField = createLabeledField("Learning Rate") {
                                makeValidatedNode(
                                    TextField(),
                                    {
                                        doubleBetweenZeroAndOne(it)
                                    },
                                    {
                                        disableProperty().bind(jobInProgressProperty)
                                        text = when (val opt = job.userOptimizer) {
                                            is Optimizer.Adam -> opt.learningRate.toString()
                                            else -> "0.001"
                                        }
                                    }
                                )
                            }

                            val beta1Field = createLabeledField("Beta 1") {
                                makeValidatedNode(
                                    TextField(),
                                    {
                                        doubleBetweenZeroAndOne(it)
                                    },
                                    {
                                        disableProperty().bind(jobInProgressProperty)
                                        text = when (val opt = job.userOptimizer) {
                                            is Optimizer.Adam -> opt.beta1.toString()
                                            else -> "0.9"
                                        }
                                    }
                                )
                            }

                            val beta2Field = createLabeledField("Beta 2") {
                                makeValidatedNode(
                                    TextField(),
                                    {
                                        doubleBetweenZeroAndOne(it)
                                    },
                                    {
                                        disableProperty().bind(jobInProgressProperty)
                                        text = when (val opt = job.userOptimizer) {
                                            is Optimizer.Adam -> opt.beta2.toString()
                                            else -> "0.999"
                                        }
                                    }
                                )
                            }

                            val epsilonField = createLabeledField("Epsilon") {
                                makeValidatedNode(
                                    TextField(),
                                    {
                                        doubleBetweenZeroAndOne(it)
                                    },
                                    {
                                        disableProperty().bind(jobInProgressProperty)
                                        text = when (val opt = job.userOptimizer) {
                                            is Optimizer.Adam -> opt.epsilon.toString()
                                            else -> "1E-7"
                                        }
                                    }
                                )
                            }

                            val amdGradField = createLabeledField("AMS Grad") {
                                makeValidatedNode(
                                    CheckBox(),
                                    {
                                        ValidationResult.Success
                                    },
                                    {
                                        disableProperty().bind(jobInProgressProperty)
                                        isSelected = when (val opt = job.userOptimizer) {
                                            is Optimizer.Adam -> opt.amsGrad
                                            else -> false
                                        }
                                    }
                                )
                            }

                            makeOptimizer = {
                                Optimizer.Adam(
                                    learningRate = learningRateField.node.text.toDouble(),
                                    beta1 = beta1Field.node.text.toDouble(),
                                    beta2 = beta2Field.node.text.toDouble(),
                                    epsilon = epsilonField.node.text.toDouble(),
                                    amsGrad = amdGradField.node.isSelected
                                )
                            }
                        })
                    })
                }
            }
        }
    }

    private fun doubleBetweenZeroAndOne(it: TextField) = try {
        val value = it.text.toDouble()
        when {
            value < 0 -> ValidationResult.Error(
                "Must be greater than 0."
            )

            value > 1 ->
                ValidationResult.Error("Must be less than 1.")

            else -> ValidationResult.Success
        }
    } catch (ex: NumberFormatException) {
        ValidationResult.Error("Must be a double.")
    }

    private fun createOutputTab(job: Job) = Tab().apply {
        isClosable = false
        text = "Output"
        content = VBox().apply {
            spacing = 10.0
            padding = Insets(5.0)
        }
    }

    private fun <T : Node> VBox.createLabeledField(label: String, makeNode: HBox.() -> T): T {
        val node: T
        children.add(HBox().apply {
            spacing = 5.0
            alignment = Pos.CENTER_LEFT

            children.add(Label(label))
            node = makeNode()
            children.add(node)
        })
        return node
    }
}
