package edu.wpi.axon.aws

import mu.KotlinLogging
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.S3Client

/**
 * Finds the S3 bucket Axon will work out of. Returns `null` if there is no matching bucket. The AWS
 * region MUST be auto-detectable from the environment (like when running on ECS). To use AWS when
 * running locally, set `AWS_REGION` to your preferred region. To not use AWS when running locally,
 * do not set `AWS_REGION`.
 *
 * @return The name of the bucket or `null` if the bucket could not be found.
 */
fun findAxonS3Bucket() = try {
    val s3Client = S3Client.builder().build()

    val bucket = s3Client.listBuckets().buckets().first {
        it.name().startsWith("axon-autogenerated-")
    }

    LOGGER.info { "Starting with S3 bucket: $bucket" }
    bucket.name()
} catch (e: SdkClientException) {
    LOGGER.info(e) { "Not loading credentials because of this exception." }
    null
}

private val LOGGER = KotlinLogging.logger { }
