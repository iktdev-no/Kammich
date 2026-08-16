package no.iktdev.kammich.services

import no.iktdev.kammich.database.models.PersistedDeviceOwner
import no.iktdev.kammich.database.models.PersistedJobOwner
import no.iktdev.kammich.database.tables.DeviceOwnerTable
import no.iktdev.kammich.database.tables.DeviceOwnerTable.toPersistedDeviceOwner
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.ImmichUsersTable
import no.iktdev.kammich.database.tables.ImmichUsersTable.toPersistedImmichUser
import no.iktdev.kammich.database.tables.ImportJobOwnerTable
import no.iktdev.kammich.database.tables.ImportJobOwnerTable.toPersistedJobOwner
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.getDeviceSerialNumber
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.device.DeviceIdentificationService
import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.models.shared.device.DeviceOwnershipSummary
import no.iktdev.kammich.models.shared.device.DeviceType
import no.iktdev.kammich.models.shared.device.ImportJobOwnershipSummary
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClaimOwnershipService(
    private val immichUserContext: ImmichUserContext,
    private val deviceIdentificationService: DeviceIdentificationService,
    private val preparationService: UploadPreparationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getDevices(): List<DeviceOwnershipSummary> {
        val devices = DevicesTable.getDevices()
        val userId = immichUserContext.getCurrentUserId()

        val owners = getDeviceOwners()
            .associate { it.deviceSerialNumber to it.immichUserId }

        val users = withTransaction {
            ImmichUsersTable.selectAll().map { it.toPersistedImmichUser() }
        }.getOrDefault(emptyList())

        return devices.map { device ->
            val ownerId = owners[device.serialNumber]
            val isPhone = deviceIdentificationService.isPhone(device)

            DeviceOwnershipSummary(
                deviceId = device.serialNumber,
                name = device.name.ifBlank { device.model ?: device.serialNumber },
                model = device.model,
                manufacturer = device.manufacturer,
                deviceType = device.deviceType?.let { DeviceType.valueOf(it) } ?: DeviceType.Unknown,
                claimable = userId != null && ownerId == null,
                claimedBy = ownerId?.let { id -> users.find { it.userId == id.toString() } }?.name
            )
        }
    }

    private fun getDeviceOwners(): List<PersistedDeviceOwner> {
        return withTransaction {
            DeviceOwnerTable.selectAll()
                .map { it.toPersistedDeviceOwner() }
        }.getOrDefault(emptyList())
    }

    fun getImportJobs(): List<ImportJobOwnershipSummary> {
        val userId = immichUserContext.getCurrentUserId()

        // Bruk associateBy for å tvinge riktige typer (UUID til UUID)
        val jobOwners = getImportJobOwners()
            .associateBy({ it.jobId }, { it.immichUserId })

        val jobStats = withTransaction {
            val countColumn = ImportedFilesTable.id.count()
            ImportedFilesTable
                .select(ImportedFilesTable.importJob, ImportedFilesTable.deviceId, countColumn)
                .groupBy(ImportedFilesTable.importJob, ImportedFilesTable.deviceId)
                .associate { row ->
                    val jobId = UUID.fromString(row[ImportedFilesTable.importJob])
                    val deviceIdVal = row[ImportedFilesTable.deviceId]
                    val deviceId = DevicesTable.getDeviceSerialNumber(deviceIdVal.value)!!
                    val count = row[countColumn].toInt()
                    jobId to Pair(deviceId, count)
                }
        }.getOrDefault(emptyMap())

        val users = withTransaction {
            ImmichUsersTable.selectAll().map { it.toPersistedImmichUser() }
        }.getOrDefault(emptyList())

        return jobStats.keys.map { jobId ->
            val ownerId = jobOwners[jobId]
            val stats = jobStats[jobId]
            val deviceId = stats?.first ?: ""
            val totalFiles = stats?.second ?: 0

            ImportJobOwnershipSummary(
                jobId = jobId.toString(),
                deviceId = deviceId,
                totalFiles = totalFiles,
                claimable = userId != null && ownerId == null,
                claimedBy = ownerId?.let { id -> users.find { it.userId == id.toString() } }?.name
            )
        }
    }


    private fun getImportJobOwners(): List<PersistedJobOwner> {
        return withTransaction {
            ImportJobOwnerTable.selectAll()
                .map { it.toPersistedJobOwner() }
        }.getOrDefault(emptyList())
    }

    fun claimDevice(userId: UUID, deviceSerial: String): Boolean {
        val deviceClaimed = withTransaction {
            DeviceOwnerTable.upsert {
                it[DeviceOwnerTable.deviceSN] = deviceSerial
                it[DeviceOwnerTable.immichUserId] = userId.toString()
            }
        }.isSuccess

        if (!deviceClaimed) {
            log.error("Klarte ikke å claime enhet $deviceSerial for bruker $userId")
        }
        // Fire and forget
        preparationService.processUnclaimedJobsForDevice(deviceSerial, userId.toString())
        return deviceClaimed
    }

    fun claimImportJob(userId: UUID, jobId: UUID): Boolean {
        val result = withTransaction {
            ImportJobOwnerTable.upsert {
                it[importJob] = jobId.toString()
                it[immichUserId] = userId.toString()
            }
        }.isSuccess

        if (!result) {
            log.error("Klarte ikke å claime jobb $jobId for bruker $userId")
        }
        preparationService.processUnclaimedJobsForUser(jobId, userId.toString())
        return result
    }

}