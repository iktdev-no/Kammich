package no.iktdev.kammich.services

import no.iktdev.kammich.database.models.PersistedDeviceOwner
import no.iktdev.kammich.database.models.PersistedJobOwner
import no.iktdev.kammich.database.tables.DeviceOwnerTable
import no.iktdev.kammich.database.tables.DeviceOwnerTable.toPersistedDeviceOwner
import no.iktdev.kammich.database.tables.DevicesTable
import no.iktdev.kammich.database.tables.DevicesTable.toPersistedDevice
import no.iktdev.kammich.database.tables.ImmichUsersTable
import no.iktdev.kammich.database.tables.ImmichUsersTable.toPersistedImmichUser
import no.iktdev.kammich.database.tables.ImportJobOwnerTable
import no.iktdev.kammich.database.tables.ImportJobOwnerTable.toPersistedJobOwner
import no.iktdev.kammich.database.tables.ImportedFilesTable
import no.iktdev.kammich.database.tables.getDeviceSerialNumber
import no.iktdev.kammich.database.withTransaction
import no.iktdev.kammich.errorNotification
import no.iktdev.kammich.immich.ImmichRepository
import no.iktdev.kammich.immich.context.ImmichUserContext
import no.iktdev.kammich.models.internal.events.ImportJobClaimedEvent
import no.iktdev.kammich.models.internal.events.ImportJobCompletedEvent
import no.iktdev.kammich.models.shared.device.DeviceOwnershipSummary
import no.iktdev.kammich.models.shared.device.DeviceType
import no.iktdev.kammich.models.shared.device.ImportJobOwnershipSummary
import no.iktdev.kammich.repository.FileRepository
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClaimOwnershipService(
    private val immichUserContext: ImmichUserContext,
    private val repo: ImmichRepository,
    private val configService: ConfigService,
    private val fileRepository: FileRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onImportCompleted(event: ImportJobCompletedEvent) {
        val userId = immichUserContext.getCurrentUserId() ?: run {
            log.warn("Ingen aktiv bruker i kontekst for import-jobb ${event.jobId}")
            return
        }

        val shouldAutoClaim = if (isSingleAndOnlyUser(userId) && configService.getConfig().autoClaimImportsWhenSingleUser) {
            true
        } else {
            userOwnsDevice(event.deviceSN, userId)
        }

        if (shouldAutoClaim) {
            val success = saveClaim(event.jobId, userId)
            if (success) {
                eventPublisher.publishEvent(ImportJobClaimedEvent(event.jobId, userId))
            }
        } else {
            log.warn("Bruker $userId eier ikke enheten (${event.deviceSN}), og systemet har flere brukere. Kan ikke auto-claime.")
        }
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
            return false
        }

        // Finn uclaimede jobb-ID-er for denne enheten og claim dem
        val unclaimedJobs = findUnclaimedJobIdsForDevice(deviceSerial)
        unclaimedJobs.forEach { jobId ->
            if (saveClaim(UUID.fromString(jobId), userId)) {
                eventPublisher.publishEvent(ImportJobClaimedEvent(UUID.fromString(jobId), userId))
            }
        }

        return true
    }

    fun claimImportJob(jobId: UUID, userId: UUID): Boolean {
        val result = saveClaim(jobId, userId)
        if (result) {
            eventPublisher.publishEvent(ImportJobClaimedEvent(jobId, userId))
        }
        return result
    }

    private fun saveClaim(jobId: UUID, userId: UUID): Boolean {
        val claimed = withTransaction {
            ImportJobOwnerTable.upsert {
                it[ImportJobOwnerTable.importJob] = jobId.toString()
                it[ImportJobOwnerTable.immichUserId] = userId.toString()
            }
        }.isSuccess

        if (!claimed) {
            log.error("Klarte ikke å lagre import-jobb eierskap for $jobId")
            eventPublisher.errorNotification(
                id = "import-claim-failed-$jobId",
                title = "Kunne ikke ta eierskap av import-jobb",
                message = "Klarte ikke å registrere eierskap for import-jobb $jobId."
            )
        }
        return claimed
    }

    private fun findUnclaimedJobIdsForDevice(deviceSn: String): List<String> {
        return withTransaction {
            val allJobIdsForDevice = fileRepository.getFilesByDeviceSn(deviceSn)
                .map { it.importJob.toString() }
                .distinct()

            if (allJobIdsForDevice.isEmpty()) return@withTransaction emptyList()

            val alreadyClaimedJobIds = ImportJobOwnerTable.selectAll()
                .where { ImportJobOwnerTable.importJob inList allJobIdsForDevice }
                .map { it[ImportJobOwnerTable.importJob] }

            allJobIdsForDevice.filter { it !in alreadyClaimedJobIds }
        }.getOrDefault(emptyList())
    }

    private fun isSingleAndOnlyUser(userId: UUID): Boolean {
        val allUsers = repo.getAllUsersWithAccesses().map { it.user }
        return allUsers.size == 1 && allUsers.first().id.toString() == userId.toString()
    }

    private fun userOwnsDevice(deviceSn: String, userId: UUID): Boolean {
        val strUsrId = userId.toString()
        return withTransaction {
            DeviceOwnerTable.selectAll()
                .where { (DeviceOwnerTable.deviceSN eq deviceSn) and (DeviceOwnerTable.immichUserId eq strUsrId) }
                .singleOrNull() != null
        }.getOrDefault(false)
    }

    fun getDevices(): List<DeviceOwnershipSummary> {
        val devices = withTransaction {
            DevicesTable.selectAll()
                .mapNotNull { it.toPersistedDevice() }
        }.getOrDefault(emptyList())

        val userId = immichUserContext.getCurrentUserId()
        val owners = getDeviceOwners().associate { it.deviceSerialNumber to it.immichUserId }
        val users = withTransaction {
            ImmichUsersTable.selectAll().map { it.toPersistedImmichUser() }
        }.getOrDefault(emptyList())

        return devices.map { device ->
            val ownerId = owners[device.serialNumber]
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
            DeviceOwnerTable.selectAll().map { it.toPersistedDeviceOwner() }
        }.getOrDefault(emptyList())
    }

    fun getImportJobs(): List<ImportJobOwnershipSummary> {
        val userId = immichUserContext.getCurrentUserId()

        // 1. Hent alle eiere og lag et rent map (jobId -> immichUserId)
        val jo = getImportJobOwners()
        jo.forEach {
            log.info("UserId: ${it.immichUserId} JobId: ${it.jobId}")
        }
        val jobOwners = jo.associate { it.jobId to it.immichUserId }
        log.info("Jobowners map: $jobOwners")

        // 2. Hent statistikk for jobbene
        val jobStats = fetchJobStats()

        // 3. Hent brukere for å kunne slå opp navn på eier
        val users = withTransaction {
            ImmichUsersTable.selectAll().map { it.toPersistedImmichUser() }
        }.getOrDefault(emptyList())

        // 4. Bygg sluttresultatet
        return jobStats.map { (jobId, stats) ->
            val ownerId = jobOwners[jobId]
            val (deviceId, totalFiles) = stats

            log.info("Mapping job $jobId -> Owner ID: $ownerId, Total files: $totalFiles")

            ImportJobOwnershipSummary(
                jobId = jobId.toString(),
                deviceId = deviceId,
                totalFiles = totalFiles,
                claimable = userId != null && ownerId == null,
                claimedBy = ownerId?.let { id -> users.find { it.userId == id.toString() } }?.name
            )
        }
    }

    private fun fetchJobStats(): Map<UUID, Pair<String, Int>> {
        return withTransaction {
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
    }

    private fun getImportJobOwners(): List<PersistedJobOwner> {
        return withTransaction {
            ImportJobOwnerTable.selectAll().map { it.toPersistedJobOwner() }
        }.getOrDefault(emptyList())
    }
}