package edu.wpi.axon.aws

import arrow.fx.IO
import arrow.fx.extensions.fx
import edu.wpi.axon.dbdata.TrainingScriptProgress
import edu.wpi.axon.tfdata.Dataset
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.koin.core.KoinComponent
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import software.amazon.awssdk.services.ec2.model.InstanceType
import software.amazon.awssdk.services.ec2.model.ShutdownBehavior

/**
 * A [TrainingScriptRunner] that runs the training script on EC2 and hosts datasets and models on
 * S3. This implementation requires that the script does not try to manage models with S3 itself:
 * this class will handle all of that. The script should just load and save the model from/to its
 * current directory.
 *
 * @param instanceType The type of the EC2 instance to run the training script on.
 */
class EC2TrainingScriptRunner(
    bucketName: String,
    private val instanceType: InstanceType
) : TrainingScriptRunner, KoinComponent {

    private val ec2 by lazy { Ec2Client.builder().build() }
    private val s3Manager = S3Manager(bucketName)

    private val nextScriptId = AtomicLong()
    private val instanceIds = mutableMapOf<Long, String>()
    private val scriptDataMap = mutableMapOf<Long, RunTrainingScriptConfiguration>()
    // Tracks whether the script entered the InProgress state at least once
    private val scriptStarted = mutableMapOf<Long, Boolean>()

    override fun startScript(runTrainingScriptConfiguration: RunTrainingScriptConfiguration): IO<Long> {
        // Check for if the script uses the CLI to manage the model in S3. This class is supposed to
        // own working with S3.
        if (runTrainingScriptConfiguration.scriptContents.contains("download_model") ||
            runTrainingScriptConfiguration.scriptContents.contains("upload_model")
        ) {
            return IO.raiseError(
                IllegalArgumentException(
                    """
                    |Cannot start the script because it interfaces with AWS:
                    |${runTrainingScriptConfiguration.scriptContents}
                    |
                    """.trimMargin()
                )
            )
        }

        // The file name for the generated script
        val scriptFileName = "${RandomStringUtils.randomAlphanumeric(20)}.py"

        return IO.fx {
            IO {
                s3Manager.uploadTrainingScript(
                    scriptFileName,
                    runTrainingScriptConfiguration.scriptContents
                )
            }.bind()

            // Reset the training progress so the script doesn't start in the completed state
            IO {
                s3Manager.resetTrainingProgress(
                    runTrainingScriptConfiguration.newModelName,
                    runTrainingScriptConfiguration.dataset.nameForS3ProgressReporting
                )
            }.bind()

            // We need to download custom datasets from S3. Example datasets will be downloaded
            // by the script using Keras.
            val downloadDatasetString = when (runTrainingScriptConfiguration.dataset) {
                is Dataset.ExampleDataset -> ""
                is Dataset.Custom ->
                    """axon download-dataset "${runTrainingScriptConfiguration.dataset.pathInS3}""""
            }

            val scriptForEC2 = """
                |#!/bin/bash
                |exec 1> >(logger -s -t ${'$'}(basename ${'$'}0)) 2>&1
                |apt update
                |apt install -y build-essential curl libcurl4-openssl-dev libssl-dev wget \
                |   python3.6 python3-pip python3-dev apt-transport-https ca-certificates \
                |   software-properties-common
                |curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
                |add-apt-repository -y "deb [arch=amd64] https://download.docker.com/linux/ubuntu bionic stable"
                |apt update
                |apt-cache policy docker-ce
                |apt install -y docker-ce
                |systemctl status docker
                |pip3 install https://github.com/wpilibsuite/axon-cli/releases/download/v0.1.10/axon-0.1.10-py2.py3-none-any.whl
                |axon download-untrained-model "${runTrainingScriptConfiguration.oldModelName}"
                |$downloadDatasetString
                |axon download-training-script "$scriptFileName"
                |docker run -v ${'$'}(eval "pwd"):/home wpilib/axon-ci:latest "/usr/bin/python3.6 /home/$scriptFileName"
                |axon upload-trained-model "${runTrainingScriptConfiguration.newModelName}"
                |shutdown -h now
                """.trimMargin()

            LOGGER.info {
                """
                |Sending script to EC2:
                |$scriptForEC2
                |
                """.trimMargin()
            }

            IO {
                ec2.runInstances {
                    it.imageId("ami-04b9e92b5572fa0d1")
                        .instanceType(instanceType)
                        .maxCount(1)
                        .minCount(1)
                        .userData(scriptForEC2.toBase64())
                        .securityGroups("axon-autogenerated-ec2-sg")
                        .instanceInitiatedShutdownBehavior(ShutdownBehavior.TERMINATE)
                        .iamInstanceProfile { it.name("axon-autogenerated-ec2-instance-profile") }
                }.let {
                    val scriptId = nextScriptId.getAndIncrement()
                    instanceIds[scriptId] = it.instances().first().instanceId()
                    scriptDataMap[scriptId] = runTrainingScriptConfiguration
                    scriptStarted[scriptId] = false
                    scriptId
                }
            }.bind()
        }
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    override fun getTrainingProgress(scriptId: Long): IO<TrainingScriptProgress> =
        if (scriptId in instanceIds.keys) {
            IO {
                val status = ec2.describeInstanceStatus {
                    it.instanceIds(
                        instanceIds[scriptId] ?: error("BUG: scriptId missing from instanceIds")
                    )
                }.instanceStatuses().firstOrNull()

                when (status?.instanceState()?.name()) {
                    InstanceStateName.RUNNING -> {
                        scriptStarted[scriptId] = true

                        val runTrainingScriptConfiguration = scriptDataMap[scriptId]
                            ?: error("BUG: scriptId missing from scriptDataMap")

                        val modelName = runTrainingScriptConfiguration.newModelName
                        val datasetName =
                            runTrainingScriptConfiguration.dataset.nameForS3ProgressReporting

                        val progressData = IO {
                            s3Manager.getTrainingProgress(modelName, datasetName)
                        }.redeem({ "0.0" }) { it }

                        LOGGER.debug { "Got progress:\n$progressData\n" }

                        progressData.redeem({ TrainingScriptProgress.InProgress(0.0) }) {
                            TrainingScriptProgress.InProgress(
                                it.toDouble() / runTrainingScriptConfiguration.epochs
                            )
                        }.unsafeRunSync()
                    }

                    InstanceStateName.SHUTTING_DOWN, InstanceStateName.TERMINATED,
                    InstanceStateName.STOPPING, InstanceStateName.STOPPED ->
                        TrainingScriptProgress.Completed

                    null -> if (scriptStarted[scriptId]
                            ?: error("BUG: scriptId missing from scriptStarted")
                    ) TrainingScriptProgress.Completed else TrainingScriptProgress.NotStarted

                    else -> TrainingScriptProgress.NotStarted
                }
            }
        } else {
            IO.raiseError(UnsupportedOperationException("Script id $scriptId not found."))
        }

    private fun String.toBase64() =
        Base64.getEncoder().encodeToString(byteInputStream().readAllBytes())

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
