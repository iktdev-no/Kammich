import { useEffect, useState } from "react";
import {
    Box,
    Typography,
    Stack,
    CircularProgress,
} from "@mui/material";

import type {
    NetworkInterface,
    TailscaleStatus,
    WirelessNetworkInterface,
} from "../types/types";

import NetworkIterfaceCard from "../features/network/NetworkInterfaceCard";
import EthernetInterfaceCard from "../features/network/EthernetInterfaceCard";
import WifiInterfaceCard from "../features/network/WifiInterfaceCard";

import { networkingApi } from "../api/requests/networking/networking";

import TailscaleIcon from "../components/icons/TailscaleIcon";
import NetworkServiceCard, {
    type NetworkServiceStatus,
} from "../features/network/NetworkServiceCard";
import { tailscaleApi } from "../api/requests/networking/tailscale";
import TailscaleStatusCard from "../features/network/tailscale/TailscaleStatusCard";
import TailscaleMagicDnsCard from "../features/network/tailscale/TailscaleMagicDnsCard";
import TailscaleServeCard from "../features/network/tailscale/TailscaleServeCard";
import TailscaleNetCheckCard from "../features/network/tailscale/TailscaleNetCheckCard";
import { useTranslation } from "react-i18next";
import PageLayout from "../components/layouts/PageLayout";

export default function Networking() {
    const { t } = useTranslation();
    const [interfaces, setInterfaces] = useState<Array<NetworkInterface>>([]);
    const [tailscale, setTailscale] = useState<TailscaleStatus | null>(null);
    const [loading, setLoading] = useState<boolean>(true);

    const fetchNetworking = async () => {
        setLoading(true);

        try {
            const [interfaces, tailscale] = await Promise.all([
                networkingApi.getInterfaces(),
                tailscaleApi.getStatus(),
            ]);

            setInterfaces(interfaces);
            setTailscale(tailscale);
        } finally {
            setLoading(false);
        }
    };

    const tailscaleStatus: NetworkServiceStatus =
        tailscale?.backendState.toLowerCase() === "running"
            ? tailscale.tun
                ? "Online"
                : "Degraded"
            : "Offline";

    useEffect(() => {
        fetchNetworking();
    }, []);

    const wifiInterfaces = interfaces.filter(
        (iface): iface is WirelessNetworkInterface =>
            iface.type === "Wifi"
    );

    const ethernetInterfaces = interfaces.filter(
        iface => iface.type === "Ethernet"
    );

    return (
        <PageLayout title={t('network.pageTitle')} >

            <Box
                sx={{
                    p: { xs: 2, md: 4 },
                    maxWidth: 800,
                    mx: "auto",
                }}
            >

                {loading && interfaces.length === 0 ? (
                    <Box
                        sx={{
                            display: "flex",
                            justifyContent: "center",
                            py: 8,
                        }}
                    >
                        <CircularProgress />
                    </Box>
                ) : (
                    <Stack sx={{ gap: 2 }}>
                        {wifiInterfaces.map(iface => (
                            <NetworkIterfaceCard
                                key={iface.interfaceName}
                                iface={iface}
                                onReset={fetchNetworking}
                            >
                                <WifiInterfaceCard iface={iface} />
                            </NetworkIterfaceCard>
                        ))}

                        {ethernetInterfaces.map(iface => (
                            <NetworkIterfaceCard
                                key={iface.interfaceName}
                                iface={iface}
                                onReset={fetchNetworking}
                            >
                                <EthernetInterfaceCard iface={iface} />
                            </NetworkIterfaceCard>
                        ))}

                        {tailscale && (
                            <NetworkServiceCard
                                icon={<TailscaleIcon />}
                                title="Tailscale"
                                hostname={tailscale.self.dnsName}
                                status={tailscaleStatus}
                            >
                                <TailscaleMagicDnsCard />
                                <TailscaleStatusCard state={tailscale} />
                                <TailscaleServeCard />
                                <TailscaleNetCheckCard />
                            </NetworkServiceCard>
                        )}
                    </Stack>
                )}
            </Box>
        </PageLayout>

    );
}