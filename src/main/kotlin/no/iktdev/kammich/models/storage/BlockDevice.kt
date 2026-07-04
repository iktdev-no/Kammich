package no.iktdev.kammich.models.storage

data class BlockDevice(
    val path: String,       // f.eks. "/dev/sdb1"
    val mountPoint: String, // f.eks. "/media/removable/sdb1"
    val serialNumber: String,
    val modelName: String
)