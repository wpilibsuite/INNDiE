package edu.wpi.inndie.aws

import java.util.Base64
import mu.KotlinLogging
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.Ec2Exception
import software.amazon.awssdk.services.ec2.model.Filter
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import software.amazon.awssdk.services.ec2.model.InstanceType
import software.amazon.awssdk.services.ec2.model.ShutdownBehavior

/**
 * Manages interacting with EC2.
 */
class EC2Manager {

    private val ec2 = Ec2Client.builder().build()

    /**
     * Starts a new instance for running a training script.
     *
     * @param scriptData The data for the EC2 instance to run when it boots. This should not
     * contain the entire training script, as that would be too much data. Instead, this script
     * should use INNDiE's CLI to download the training script from S3 at runtime.
     * @param instanceType The type of the instance to start.
     * @return The ID of the instance that was started.
     */
    fun startTrainingInstance(scriptData: String, instanceType: InstanceType): String {
        val runInstancesResponse = ec2.runInstances {
            it.imageId("ami-04b9e92b5572fa0d1")
                .instanceType(instanceType)
                .maxCount(1)
                .minCount(1)
                .userData(scriptData.toBase64())
                .securityGroups("inndie-autogenerated-ec2-sg")
                .instanceInitiatedShutdownBehavior(ShutdownBehavior.TERMINATE)
                .iamInstanceProfile { it.name("inndie-autogenerated-ec2-instance-profile") }
        }

        return runInstancesResponse.instances().first().instanceId()
    }

    /**
     * Gets the state of the instance. This includes:
     *  - Instances that are running
     *  - Instances that are not running but are not shut down or terminated
     *
     * @param instanceId The ID of the instance.
     * @return The state of the instance.
     */
    fun getInstanceState(instanceId: String): InstanceStateName? {
        return try {
            ec2.describeInstanceStatus {
                it.instanceIds(instanceId)
                    .includeAllInstances(true)
                    .filters(
                        Filter.builder().name("instance-state-name").values(
                            "pending",
                            "running",
                            "shutting-down",
                            "stopping"
                        ).build()
                    )
            }.instanceStatuses().firstOrNull()?.instanceState()?.name()
        } catch (ex: Ec2Exception) {
            LOGGER.debug(ex) { "Failed to get instance status." }
            null
        }
    }

    /**
     * Terminates the instance. This operation is idempotent.
     *
     * @param instanceId The ID of the instance.
     */
    fun terminateInstance(instanceId: String) {
        ec2.terminateInstances {
            it.instanceIds(instanceId)
        }
    }

    private fun String.toBase64() =
        Base64.getEncoder().encodeToString(byteInputStream().readAllBytes())

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
