package no.iktdev.kammich.system.network.al

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import no.iktdev.kammich.models.shared.tailscale.TailscaleDns
import no.iktdev.kammich.models.shared.tailscale.TailscaleDnsServer
import no.iktdev.kammich.models.shared.tailscale.TailscaleNetcheck
import no.iktdev.kammich.models.shared.tailscale.TailscaleServe
import no.iktdev.kammich.models.shared.tailscale.TailscaleStatus
import no.iktdev.kammich.models.shared.tailscale.TailscaleStatusSelf
import no.iktdev.kammich.system.SysCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class TailAL(private val exec: SysCommand): ITailAL {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun isSupported(): Boolean {
        return exec.nonSudo("which", "tailscale").isSuccess()
    }

    override fun getStatus(): TailscaleStatus? {
        val out = exec.nonSudo(
            "tailscale", "status", "--active", "--self", "--json"
        ).getOrNull() ?: run {
            log.error("Could not get Tailscale status")
            return null
        }

        return try {
            val root = JsonParser.parseString(out).asJsonObject
            val self = root.getAsJsonObject("Self")
            val selfStatus = TailscaleStatusSelf(
                online = self.get("Online").asBoolean,
                exitNode = self.get("ExitNode").asBoolean,
                exitNodeOption = self.get("ExitNodeOption").asBoolean,
                relay = self.get("Relay").asString,
                dnsName = self.get("DNSName").asString
            )
            TailscaleStatus(
                self = selfStatus,
                version = root.get("Version").asString,
                tun = root.get("TUN").asBoolean,
                backendState = root.get("BackendState").asString,
            )


        } catch (e: Exception) {
            log.error("Could not parse Tailscale status", e)
            null
        }
    }


    override fun getServe(): List<TailscaleServe> {
        val out = exec.nonSudo(
            "tailscale", "serve", "status", "--json"
        ).getOrNull() ?: run {
            log.error("Could not get Tailscale serve status")
            return emptyList()
        }

        return try {
            val root = JsonParser.parseString(out).asJsonObject
            val web = root.getAsJsonObject("Web") ?: return emptyList()

            web.entrySet().flatMap { (address, config) ->
                val host = address.substringBeforeLast(":")
                val port = address.substringAfterLast(":").toIntOrNull()
                    ?: return@flatMap emptyList()

                val handlers = config.asJsonObject
                    .getAsJsonObject("Handlers")
                    ?: return@flatMap emptyList()

                handlers.entrySet().mapNotNull { (path, handler) ->
                    val proxy = handler.asJsonObject
                        .get("Proxy")
                        ?.asString
                        ?: return@mapNotNull null

                    TailscaleServe(
                        hostname = host,
                        port = port,
                        proxy = proxy,
                        https = root
                            .getAsJsonObject("TCP")
                            ?.getAsJsonObject(port.toString())
                            ?.get("HTTPS")
                            ?.asBoolean
                            ?: false,
                        path = path
                    )
                }
            }
        } catch (e: Exception) {
            log.error("Could not parse Tailscale serve status", e)
            emptyList()
        }
    }

    override fun getNetcheck(): TailscaleNetcheck? {
        val out = exec.nonSudo(
            "tailscale", "netcheck", "--format", "json"
        ).getOrNull() ?: run {
            log.error("Could not get Tailscale netcheck")
            return null
        }

        return try {
            if (out.isBlank()) {
                log.error("Could not find JSON in Tailscale netcheck output")
                return null
            }

            val root = JsonParser.parseString(out).asJsonObject

            TailscaleNetcheck(
                now = Instant.parse(root.get("Now").asString),
                udp = root.get("UDP").asBoolean,
                ipv6 = root.get("IPv6").asBoolean,
                ipv4 = root.get("IPv4").asBoolean,
                ipv6CanSend = root.get("IPv6CanSend").asBoolean,
                ipv4CanSend = root.get("IPv4CanSend").asBoolean,
                osHasIpv6 = root.get("OSHasIPv6").asBoolean,
                icmpv4 = root.get("ICMPv4").asBoolean,
                mappingVariesByDestIp = root.get("MappingVariesByDestIP").asBoolean,
                upnp = root.get("UPnP").asBoolean,
                pmp = root.get("PMP").asBoolean,
                pcp = root.get("PCP").asBoolean,
                preferredDerp = root.get("PreferredDERP")?.asInt,
                regionLatency = parseLatency(root, "RegionLatency"),
                regionV4Latency = parseLatency(root, "RegionV4Latency"),
                regionV6Latency = parseLatency(root, "RegionV6Latency"),
                globalV4 = root.get("GlobalV4")?.asString?.ifBlank { null },
                globalV6 = root.get("GlobalV6")?.asString?.ifBlank { null },
                captivePortal = root.get("CaptivePortal")
                    ?.takeUnless { it.isJsonNull }
                    ?.asString
            )
        } catch (e: Exception) {
            log.error("Could not parse Tailscale netcheck", e)
            null
        }
    }

    override fun getDns(): TailscaleDns? {
        val out = exec.nonSudo(
            "tailscale", "dns", "status", "--json"
        ).getOrNull() ?: run {
            log.error("Could not get Tailscale DNS status")
            return null
        }

        return try {
            val root = JsonParser.parseString(out).asJsonObject
            val tailnet = root.getAsJsonObject("CurrentTailnet")

            val splitDnsRoutes = root
                .getAsJsonObject("SplitDNSRoutes")
                ?.entrySet()
                ?.associate { (domain, servers) ->
                    domain to servers.asJsonArray.map {
                        TailscaleDnsServer(
                            address = it.asJsonObject
                                .get("Addr")
                                .asString
                        )
                    }
                }
                ?: emptyMap()

            TailscaleDns(
                tailscaleDns = root.get("TailscaleDNS").asBoolean,
                magicDnsEnabled = tailnet
                    ?.get("MagicDNSEnabled")
                    ?.asBoolean
                    ?: false,
                magicDnsSuffix = tailnet
                    ?.get("MagicDNSSuffix")
                    ?.asString,
                selfDnsName = tailnet
                    ?.get("SelfDNSName")
                    ?.asString,
                splitDnsRoutes = splitDnsRoutes,
                searchDomains = root
                    .getAsJsonArray("SearchDomains")
                    ?.map { it.asString }
                    ?: emptyList(),
                certDomains = root
                    .getAsJsonArray("CertDomains")
                    ?.map { it.asString }
                    ?: emptyList(),
                exitNodeFilteredSet = root
                    .getAsJsonArray("ExitNodeFilteredSet")
                    ?.map { it.asString }
                    ?: emptyList()
            )
        } catch (e: Exception) {
            log.error("Could not parse Tailscale DNS status", e)
            null
        }
    }


    private fun parseLatency(
        root: JsonObject,
        field: String
    ): Map<Int, Duration> {
        val obj = root.getAsJsonObject(field) ?: return emptyMap()

        return obj.entrySet().associate { (region, latency) ->
            region.toInt() to Duration.ofNanos(latency.asLong)
        }
    }
}