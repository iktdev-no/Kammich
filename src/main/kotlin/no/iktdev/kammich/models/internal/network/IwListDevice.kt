package no.iktdev.kammich.models.internal.network

import com.google.gson.annotations.SerializedName

// --- MODELLER FOR IW LIST ---
data class IwListDevice(
    @SerializedName("wiphy") val wiphy: String = "",
    @SerializedName("interfaces") val interfaces: List<String> = emptyList(),
    @SerializedName("supported_modes") val supportedModes: List<String> = emptyList(),
    @SerializedName("valid_interface_combinations") val validCombinations: List<IwInterfaceCombination> = emptyList()
)

data class IwInterfaceCombination(
    @SerializedName("combinations") val combinations: List<IwCombinationDetail> = emptyList()
)

data class IwCombinationDetail(
    @SerializedName("types") val types: List<String> = emptyList(),
    @SerializedName("max_interfaces") val maxInterfaces: Int = 0,
    @SerializedName("channels") val channels: Int = 0
)

// --- MODELLER FOR IW SCAN ---
data class IwScanItem(
    @SerializedName("bssid")
    val bssid: String = "",

    @SerializedName("signal_dbm")
    val signalDbm: Double = 0.0,

    @SerializedName("capability")
    val capability: String = "",

    // SSID fallbacks
    @SerializedName("information_elements_from_probe_response_frame_ssid")
    val probeSsid: String? = null,

    @SerializedName("information_elements_from_beacon_frame_ssid")
    val beaconSsid: String? = null,

    // Authentication Suites fallbacks
    @SerializedName("information_elements_from_probe_response_frame_authentication_suites")
    val probeAuthSuites: String? = null,

    @SerializedName("information_elements_from_beacon_frame_authentication_suites")
    val beaconAuthSuites: String? = null,

    // RSN fallbacks
    @SerializedName("information_elements_from_probe_response_frame_rsn")
    val probeRsn: String? = null,

    @SerializedName("information_elements_from_beacon_frame_rsn")
    val beaconRsn: String? = null
) {
    // Hjelpefunksjoner for å hente ut verdi uavhengig av hvilken ramme jc parset fra
    val ssid: String
        get() = probeSsid ?: beaconSsid ?: ""

    val authSuites: String
        get() = probeAuthSuites ?: beaconAuthSuites ?: ""

    val hasRsn: Boolean
        get() = probeRsn != null || beaconRsn != null
}
