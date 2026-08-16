package no.iktdev.kammich.system.network.wifi.parser

import no.iktdev.kammich.system.network.v1.wifi.parser.WifiPhyInfoParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WifiPhyInfoParserTest {

    private val parser = WifiPhyInfoParser()

    @Test
    fun shouldReturnSupportOnAP() {
        val raw = """
            Wiphy phy0
                    wiphy index: 0
                    max # scan SSIDs: 20
                    max scan IEs length: 413 bytes
                    max # sched scan SSIDs: 20
                    max # match sets: 11
                    Retry short limit: 7
                    Retry long limit: 4
                    Coverage class: 0 (up to 0m)
                    Device supports RSN-IBSS.
                    Device supports AP-side u-APSD.
                    Supported Ciphers:
                            * WEP40 (00-0f-ac:1)
                            * WEP104 (00-0f-ac:5)
                            * TKIP (00-0f-ac:2)
                            * CCMP-128 (00-0f-ac:4)
                            * CMAC (00-0f-ac:6)
                    Available Antennas: TX 0x3 RX 0x3
                    Configured Antennas: TX 0x3 RX 0x3
                    Supported interface modes:
                             * IBSS
                             * managed
                             * AP
                             * AP/VLAN
                             * monitor
                             * P2P-client
                             * P2P-GO
                             * P2P-device
                    Band 1:
                            Capabilities: 0x11ef
                                    RX LDPC
                                    HT20/HT40
                                    SM Power Save disabled
                                    RX HT20 SGI
                                    RX HT40 SGI
                                    TX STBC
                                    RX STBC 1-stream
                                    Max AMSDU length: 3839 bytes
                                    DSSS/CCK HT40
                            Maximum RX AMPDU length 65535 bytes (exponent: 0x003)
                            Minimum RX AMPDU time spacing: 4 usec (0x05)
                            HT Max RX data rate: 300 Mbps
                            HT TX/RX MCS rate indexes supported: 0-15
                            Bitrates (non-HT):
                                    * 1.0 Mbps
                                    * 54.0 Mbps
                            Frequencies:
                                    * 2412.0 MHz [1] (22.0 dBm)
                    Band 2:
                            Capabilities: 0x11ef
                                    RX LDPC
                                    HT20/HT40
                                    SM Power Save disabled
                                    RX HT20 SGI
                                    RX HT40 SGI
                                    TX STBC
                                    RX STBC 1-stream
                                    Max AMSDU length: 3839 bytes
                                    DSSS/CCK HT40
                            Maximum RX AMPDU length 65535 bytes (exponent: 0x003)
                            Minimum RX AMPDU time spacing: 4 usec (0x05)
                            HT Max RX data rate: 300 Mbps
                            HT TX/RX MCS rate indexes supported: 0-15
                            VHT Capabilities (0x038071b0):
                                    Max MPDU length: 3895
                                    Supported Channel Width: neither 160 nor 80+80
                                    RX LDPC
                                    short GI (80 MHz)
                                    TX STBC
                                    SU Beamformee
                            VHT RX MCS set:
                                    1 streams: MCS 0-9
                                    8 streams: not supported
                            VHT RX highest supported: 0 Mbps
                            VHT TX MCS set:
                                    1 streams: MCS 0-9
                            VHT TX highest supported: 0 Mbps
                            VHT extended NSS: supported
                            Bitrates (non-HT):
                                    * 6.0 Mbps
                                    * 54.0 Mbps
                            Frequencies:
                                    * 5180.0 MHz [36] (22.0 dBm) (no IR)
                                    * 5200.0 MHz [40] (22.0 dBm) (no IR)
                                    * 5220.0 MHz [44] (22.0 dBm) (no IR)
                    Supported commands:
                             * new_interface
                             * set_interface
                    WoWLAN support:
                             * wake up on disconnect
                             * wake up on magic packet
                    software interface modes (can always be added):
                             * AP/VLAN
                             * monitor
                    valid interface combinations:
                             * #{ managed } <= 1, #{ P2P-client, P2P-GO } <= 1, #{ P2P-device } <= 1,
                               total <= 3, #channels <= 2
                             * #{ managed } <= 1, #{ AP, P2P-client, P2P-GO } <= 1, #{ P2P-device } <= 1,
                               total <= 3, #channels <= 1

        """.trimIndent()

        val result = parser.parse(raw)

        // Sett breakpoints her og inspiser 'result'
        assertTrue(result.supportsAP, "Should support AP")
        assertTrue(result.isConcurrent, "Should support concurrency (managed + AP)")
        assertTrue(result.sameChannelConstraint, "Should have channel constraint")

    }



}