package edu.wpi.inndie.db

import edu.wpi.inndie.db.data.TrainingScriptProgress
import edu.wpi.inndie.db.data.nextJob
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.nulls.shouldBeNull
import io.kotlintest.shouldBe
import java.io.File
import kotlin.random.Random
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class JobDbTest {

    @Test
    fun `create test`(@TempDir tempDir: File) {
        val db = createDb(tempDir)

        val newJob = Random.nextJob(db)

        transaction {
            Jobs.select { Jobs.nameCol eq newJob.name }
                .map { Jobs.toDomain(it) }
                .shouldContainExactly(newJob)
        }
    }

    @Test
    fun `update test`(@TempDir tempDir: File) {
        val db = createDb(tempDir)
        val job = Random.nextJob(db)

        db.update(job.id, name = "Test")

        transaction {
            Jobs.select { Jobs.id eq job.id }
                .map { Jobs.toDomain(it) }
                .let {
                    it.shouldHaveSize(1)
                    it.first().name.shouldBe("Test")
                }
        }
    }

    @Test
    fun `find by name test`(@TempDir tempDir: File) {
        val db = createDb(tempDir)

        val job = Random.nextJob(db)

        db.findByName(job.name).shouldBe(job)
    }

    @Test
    fun `count test`(@TempDir tempDir: File) {
        val db = createDb(tempDir)

        db.count().shouldBe(0)

        Random.nextJob(db)

        db.count().shouldBe(1)
    }

    @Test
    fun `remove test`(@TempDir tempDir: File) {
        val db = createDb(tempDir)

        val job = Random.nextJob(db)

        db.remove(job)
        db.findByName(job.name).shouldBeNull()
    }

    @Test
    fun `test fetching running jobs`(@TempDir tempDir: File) {
        val db = createDb(tempDir)

        val runningJobs = listOf(
            Random.nextJob(db, status = TrainingScriptProgress.Creating),
            Random.nextJob(db, status = TrainingScriptProgress.Initializing),
            Random.nextJob(
                db, status = TrainingScriptProgress.InProgress(
                    0.2,
                    """
                    epoch
                    0
                    1
                    2
                    """.trimIndent()
                )
            )
        )

        // Jobs that are not running
        listOf(
            Random.nextJob(db, status = TrainingScriptProgress.NotStarted),
            Random.nextJob(db, status = TrainingScriptProgress.Completed),
            Random.nextJob(db, status = TrainingScriptProgress.Error("some error msg"))
        )

        val runningJobsFromDb = db.fetchRunningJobs()
        runningJobsFromDb.shouldContainExactlyInAnyOrder(runningJobs)
    }

    private fun createDb(tempDir: File) = JobDb(
        Database.connect(
            url = "jdbc:h2:file:${tempDir.resolve("test.db")}",
            driver = "org.h2.Driver"
        )
    )
}
