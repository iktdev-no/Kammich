package no.iktdev.kammich.models

import com.google.gson.annotations.SerializedName

data class LsblkResponse(
    @SerializedName("blockdevices") val devices: List<LsblkDevice>
)

data class LsblkDevice(
    val name: String,
    val path: String?,
    val mountpoint: String?,
    val model: String?,
    val serial: String?,
    val type: String, // "disk" eller "part"
    @SerializedName("tran") val transport: String?,
    val pttype: String?,
    val children: List<LsblkPartition>? = emptyList() // Rekursiv definisjon
)

data class LsblkPartition(
    val name: String,
    val path: String?,
    val mountpoint: String?,
    val model: String?,
    val serial: String?,
    val type: String, // "disk" eller "part"
    @SerializedName("tran") val transport: String?,
    val pttype: String?,
)