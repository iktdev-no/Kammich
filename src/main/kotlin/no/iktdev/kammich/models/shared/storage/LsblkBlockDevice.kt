package no.iktdev.kammich.models.shared.storage

data class LsblkBlockDevice(
    val name: String,
    val path: String,       // f.eks. "/dev/sdb1"
    val mountPoint: String?, // f.eks. "/media/removable/sdb1"
    val serialNumber: String,
    val modelName: String,
    val mounted: Boolean,
    val transport: Transport,
)