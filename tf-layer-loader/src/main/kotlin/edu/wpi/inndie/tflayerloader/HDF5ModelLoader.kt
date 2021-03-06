@file:Suppress("UNCHECKED_CAST", "StringLiteralDuplication")

package edu.wpi.inndie.tflayerloader

import arrow.core.Left
import arrow.core.Right
import arrow.fx.IO
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import edu.wpi.inndie.tfdata.Model
import edu.wpi.inndie.tfdata.SerializableEitherDLd
import edu.wpi.inndie.tfdata.SerializableEitherITii
import edu.wpi.inndie.tfdata.SerializableTuple2II
import edu.wpi.inndie.tfdata.layer.Activation
import edu.wpi.inndie.tfdata.layer.Constraint
import edu.wpi.inndie.tfdata.layer.DataFormat
import edu.wpi.inndie.tfdata.layer.Initializer
import edu.wpi.inndie.tfdata.layer.Interpolation
import edu.wpi.inndie.tfdata.layer.Layer
import edu.wpi.inndie.tfdata.layer.PoolingPadding
import edu.wpi.inndie.tfdata.layer.Regularizer
import edu.wpi.inndie.util.singleAssign
import io.jhdf.HdfFile
import java.io.File

/**
 * Loads TensorFlow layers from an HDF5 file.
 */
internal class HDF5ModelLoader(
    private val layersToGraph: LayersToGraph
) : ModelLoader {

    /**
     * Load layers from the [file].
     *
     * @param file The file to load from.
     * @return The layers in the file.
     */
    override fun load(file: File): IO<Model> = IO {
        HdfFile(file).use {
            val config = it.getAttribute("model_config").data as String
            val data = Parser.default().parse(config.byteInputStream()) as JsonObject
            parseModel(data)
            // TODO: Post-process the Model to get rid of any ModelLayer instances (basically,
            //  flatten the Model).
        }
    }

    private fun parseModel(json: JsonObject): Model =
        when (val className = json["class_name"] as String) {
            "Sequential" -> parseSequentialModel(json)
            "Model" -> parseGeneralModel(json)
            else -> throw IllegalStateException("Unknown model class $className")
        }

    private fun parseSequentialModel(json: JsonObject): Model.Sequential {
        val config = json["config"] as JsonObject

        var batchInputShape: List<Int?> by singleAssign()

        val layers = (config["layers"] as JsonArray<JsonObject>).mapTo(mutableSetOf()) {
            val layer = parseLayer(it["class_name"] as String, it)

            ((it["config"] as JsonObject)["batch_input_shape"] as JsonArray<Int?>?)?.let {
                batchInputShape = it.toList()
            }

            parseMetaLayer(layer, it["config"] as JsonObject)
        }

        return Model.Sequential(
            config["name"] as String,
            batchInputShape,
            layers.toSet()
        )
    }

    private fun parseGeneralModel(json: JsonObject): Model.General {
        val config = json["config"] as JsonObject

        val inputLayerIds = (config["input_layers"] as JsonArray<JsonArray<Any>>)
            .mapTo(mutableSetOf()) {
                it.first() as String
            }

        val outputLayerIds = (config["output_layers"] as JsonArray<JsonArray<Any>>)
            .mapTo(mutableSetOf()) {
                it.first() as String
            }

        val layers = (config["layers"] as JsonArray<JsonObject>).mapTo(mutableSetOf()) {
            val layer = parseLayer(it["class_name"] as String, it)
            parseMetaLayer(layer, it["config"] as JsonObject)
        }

        return Model.General(
            name = config["name"] as String,
            input = inputLayerIds.mapTo(mutableSetOf()) { inputId ->
                Model.General.InputData(
                    inputId,
                    (layers.first {
                        it.name == inputId &&
                            it is Layer.MetaLayer.UntrainableLayer &&
                            it.layer is Layer.InputLayer
                    }.layer as Layer.InputLayer).batchInputShape
                )
            },
            layers = layersToGraph.convertToGraph(layers).fold({ error(it) }, { it }),
            output = outputLayerIds.mapTo(mutableSetOf()) { Model.General.OutputData(it) }
        )
    }

    private fun parseMetaLayer(layer: Layer, json: JsonObject): Layer.MetaLayer {
        return when (layer) {
            // Don't wrap a MetaLayer more than once
            is Layer.MetaLayer -> layer

            else -> when (val trainable = json["trainable"] as Boolean?) {
                null -> layer.untrainable()
                else -> layer.isTrainable(trainable)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLayer(className: String, data: JsonObject): Layer {
        val json = data["config"] as JsonObject
        val name = json["name"] as String
        return when (className) {
            "Sequential", "Model" -> Layer.ModelLayer(name, data.inboundNodes(), parseModel(data))

            "InputLayer" -> Layer.InputLayer(
                name,
                (json["batch_input_shape"] as JsonArray<Int?>).toList().let {
                    if (it.first() != null) {
                        throw IllegalArgumentException("First element of InputLayer batch_input_shape was not null: " + it.joinToString())
                    }
                    it
                }
            )

            "BatchNormalization", "BatchNormalizationV1" -> Layer.BatchNormalization(
                name,
                data.inboundNodes(),
                (json["axis"] as JsonArray<Int>).let {
                    if (it.size != 1) {
                        throw IllegalArgumentException("Size of array was not 1")
                    }
                    it.first()
                },
                json["momentum"].double(),
                json["epsilon"].double(),
                json["center"] as Boolean,
                json["scale"] as Boolean,
                json["beta_initializer"].initializer(),
                json["gamma_initializer"].initializer(),
                json["moving_mean_initializer"].initializer(),
                json["moving_variance_initializer"].initializer(),
                json["beta_regularizer"].regularizer(),
                json["gamma_regularizer"].regularizer(),
                json["beta_contraint"].constraint(),
                json["gamma_contraint"].constraint(),
                json["renorm"] as Boolean? ?: false,
                json["renorm_clipping"] as Map<String, Double>?,
                json["renorm_momentum"] as Double?,
                json["fused"] as Boolean?,
                json["virtual_batch_size"] as Int?
            )

            "AvgPool2D", "AveragePooling2D" -> Layer.AveragePooling2D(
                name,
                data.inboundNodes(),
                json["pool_size"].tuple2OrInt(),
                json["strides"].tuple2OrIntOrNull(),
                json["padding"].poolingPadding(),
                json["data_format"].dataFormatOrNull()
            )

            "Conv2D" -> Layer.Conv2D(
                name,
                data.inboundNodes(),
                json["filters"] as Int,
                (json["kernel_size"] as JsonArray<Int>).let {
                    SerializableTuple2II(
                        it[0],
                        it[1]
                    )
                },
                parseActivation(json)
            )

            "Dense" -> Layer.Dense(
                name,
                data.inboundNodes(),
                json["units"] as Int,
                parseActivation(json),
                json["use_bias"] as Boolean,
                json["kernel_initializer"].initializer(),
                json["bias_initializer"].initializer(),
                json["kernel_regularizer"].regularizer(),
                json["bias_regularizer"].regularizer(),
                json["activity_regularizer"].regularizer(),
                json["kernel_constraint"].constraint(),
                json["bias_constraint"].constraint()
            )

            "Dropout" -> Layer.Dropout(
                name,
                data.inboundNodes(),
                json["rate"].double(),
                (json["noise_shape"] as JsonArray<Int>?)?.toList()?.let {
                    throw IllegalStateException(
                        "noise_shape was not null (this isn't bad): ${it.joinToString()}"
                    )
                },
                json["seed"] as Int?
            )

            "Flatten" -> Layer.Flatten(
                name,
                data.inboundNodes(),
                json["data_format"].dataFormatOrNull()
            )

            "GlobalAveragePooling2D", "GlobalAvgPool2D" -> Layer.GlobalAveragePooling2D(
                name,
                data.inboundNodes(),
                json["data_format"].dataFormatOrNull()
            )

            "GlobalMaxPooling2D", "GlobalMaxPool2D" -> Layer.GlobalMaxPooling2D(
                name,
                data.inboundNodes(),
                json["data_format"].dataFormatOrNull()
            )

            "MaxPool2D", "MaxPooling2D" -> Layer.MaxPooling2D(
                name,
                data.inboundNodes(),
                json["pool_size"].tuple2OrInt(),
                json["strides"].tuple2OrIntOrNull(),
                json["padding"].poolingPadding(),
                json["data_format"].dataFormatOrNull()
            )

            "SpatialDropout2D" -> Layer.SpatialDropout2D(
                name,
                data.inboundNodes(),
                json["rate"].double(),
                json["data_format"].dataFormatOrNull()
            )

            "UpSampling2D" -> Layer.UpSampling2D(
                name,
                data.inboundNodes(),
                json["size"].tuple2OrInt(),
                json["data_format"].dataFormatOrNull(),
                json["interpolation"].interpolation()
            )

            else -> Layer.UnknownLayer(
                name,
                data.inboundNodes()
            )
        }
    }

    private fun parseActivation(json: JsonObject): Activation =
        when (val name = json["activation"] as String) {
            "linear" -> Activation.Linear
            "relu" -> Activation.ReLu
            "softmax" -> Activation.SoftMax
            else -> Activation.UnknownActivation(name)
        }
}

private fun Any?.initializer(): Initializer {
    require(this != null)
    require(this is JsonObject)
    val config = this["config"] as JsonObject
    return when (this["class_name"]) {
        "Constant" -> Initializer.Constant(
            SerializableEitherDLd.fromEither(
                when (val value = config["value"]) {
                    is Number -> Left(value.toDouble())

                    // This works for list, tuple, and nparray
                    is JsonArray<*> -> Right((value as JsonArray<Number>).map { it.toDouble() })

                    else -> throw IllegalStateException("Unknown Constant initializer value: $value")
                }
            )
        )

        "Identity" -> Initializer.Identity(config["gain"].double())

        "Zeros" -> Initializer.Zeros
        "Ones" -> Initializer.Ones

        "Orthogonal" -> Initializer.Orthogonal(config["gain"].double(), config["seed"] as Int?)

        "RandomNormal" -> Initializer.RandomNormal(
            config["mean"].double(),
            config["stddev"].double(),
            config["seed"] as Int?
        )

        "RandomUniform" -> Initializer.RandomUniform(
            config["minval"].randomUniformVal(),
            config["maxval"].randomUniformVal(),
            config["seed"] as Int?
        )

        "TruncatedNormal" -> Initializer.TruncatedNormal(
            config["mean"].double(),
            config["stddev"].double(),
            config["seed"] as Int?
        )

        "VarianceScaling" -> Initializer.VarianceScaling(
            config["scale"].double(),
            config["mode"].varianceScalingMode(),
            config["distribution"].varianceScalingDistribution(),
            config["seed"] as Int?
        )

        "GlorotNormal" -> Initializer.GlorotNormal(config["seed"] as Int?)

        "GlorotUniform" -> Initializer.GlorotUniform(config["seed"] as Int?)

        else -> throw IllegalStateException("Unknown initializer: ${this.entries.joinToString()}")
    }
}

private fun Any?.double() = (this as Number).toDouble()

private fun Any?.randomUniformVal() = SerializableEitherDLd.fromEither(
    when (this) {
        is Double -> Left(this)
        is JsonArray<*> -> Right((this as JsonArray<Double>).toList())
        else -> throw IllegalStateException("Unknown RandomUniform val: $this")
    }
)

private fun Any?.varianceScalingMode() = when (this) {
    "fan_in" -> Initializer.VarianceScaling.Mode.FanIn
    "fan_out" -> Initializer.VarianceScaling.Mode.FanOut
    "fan_avg" -> Initializer.VarianceScaling.Mode.FanAvg
    else -> throw IllegalStateException("Unknown VarianceScaling mode: $this")
}

private fun Any?.varianceScalingDistribution() = when (this) {
    "normal" -> Initializer.VarianceScaling.Distribution.Normal
    "uniform" -> Initializer.VarianceScaling.Distribution.Uniform
    "truncated_normal" -> Initializer.VarianceScaling.Distribution.TruncatedNormal
    "untruncated_normal" -> Initializer.VarianceScaling.Distribution.UntruncatedNormal
    else -> throw IllegalStateException("Unknown VarianceScaling distribution: $this")
}

private fun Any?.regularizer(): Regularizer? =
    if (this == null) {
        null
    } else {
        require(this is JsonObject)
        val config = this["config"] as JsonObject
        when (this["class_name"]) {
            "L1L2" -> Regularizer.L1L2(config["l1"].double(), config["l2"].double())
            else ->
                throw IllegalStateException("Unknown regularizer: ${this.entries.joinToString()}")
        }
    }

private fun Any?.constraint(): Constraint? =
    if (this == null) {
        null
    } else {
        require(this is JsonObject)
        val config = this["config"] as JsonObject
        when (this["class_name"]) {
            "MaxNorm" -> Constraint.MaxNorm(config["max_value"].double(), config["axis"] as Int)

            "MinMaxNorm" -> Constraint.MinMaxNorm(
                config["min_value"].double(),
                config["max_value"].double(),
                config["rate"].double(),
                config["axis"] as Int
            )

            "NonNeg" -> Constraint.NonNeg

            "UnitNorm" -> Constraint.UnitNorm(config["axis"] as Int)

            else ->
                throw IllegalStateException("Unknown constraint: ${this.entries.joinToString()}")
        }
    }

private fun Any?.poolingPadding(): PoolingPadding = when (this as? String) {
    "valid" -> PoolingPadding.Valid
    "same" -> PoolingPadding.Same
    else -> throw IllegalArgumentException("Not convertible: $this")
}

private fun Any?.dataFormatOrNull(): DataFormat? = when (this as? String) {
    "channels_first" -> DataFormat.ChannelsFirst
    "channels_last" -> DataFormat.ChannelsLast
    null -> null
    else -> throw IllegalArgumentException("Not convertible: $this")
}

private fun Any?.interpolation(): Interpolation = when (this as? String) {
    // Null in versions < v1.15.0 (TF bug). Use nearest as the default
    null, "nearest" -> Interpolation.Nearest
    "bilinear" -> Interpolation.Bilinear
    else -> throw IllegalArgumentException("Not convertible: $this")
}

private fun Any?.tuple2OrInt() = SerializableEitherITii.fromEither(
    when {
        this is Int -> Left(this)

        this as? JsonArray<Int> != null -> {
            require(this.size == 2)
            Right(SerializableTuple2II(this[0], this[1]))
        }

        else -> throw IllegalArgumentException("Not convertible: $this")
    }
)

private fun Any?.tuple2OrIntOrNull() = when {
    this is Int -> Left(this)

    this as? JsonArray<Int> != null -> {
        require(this.size == 2)
        Right(SerializableTuple2II(this[0], this[1]))
    }

    else -> if (this == null) {
        null
    } else {
        throw IllegalArgumentException("Not convertible: $this")
    }
}?.let { SerializableEitherITii.fromEither(it) }

@Suppress("UNCHECKED_CAST")
private fun JsonObject.inboundNodes(): Set<String>? {
    // None is valid for Sequential models
    val inboundNodes = this["inbound_nodes"] ?: return null
    inboundNodes as JsonArray<JsonArray<JsonArray<Any>>>
    require(inboundNodes.size == 1)
    return inboundNodes[0].mapTo(mutableSetOf()) {
        it[0] as String
    }
}
