import React, { useEffect, useState } from "react";
import {
    Box,
    Typography,
    Button,
    List,
    CircularProgress,
    Collapse,
    useTheme,
    IconButton,
    Tooltip
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import WifiOffIcon from '@mui/icons-material/WifiOff';

import { useSseSelector } from "../../sse/useSseSelector";
import type { InterfaceActiveState, WifiNetwork, WifiNetworkConnection, WifiNetworkScan, WirelessInterface } from "../../types/types";
import { getInterfaces, getNewNetworks } from "../../api/requests/networking/connection";
import WifiNetworkCard from "../../components/network/WifiNetwork";
import { VisibilityIcon } from "../../components/icons/VisibilityIcon";

export default function WifiSettings() {
    const theme = useTheme();
    const [activeInterface, setActiveInterface] = useState<string>("");
    const [expandedInterface, setExpandedInterface] = useState<string | false>(false);

    // Holder styr på om skjulte nettverk vises per grensesnitt (interfaceName -> boolean)
    const [showHiddenMap, setShowHiddenMap] = useState<Record<string, boolean>>({});

    // Data fra SSE
    const [interfaces, setInterfaces] = useState<WirelessInterface[]>([]);
    const [isLoadingInterfaces, setIsLoadingInterfaces] = useState(true);

    const wifiScans = useSseSelector(state => state.wifiScans) || [];
    const wifiConnections = useSseSelector(state => state.wifiConnections) || [];

    useEffect(() => {
        async function loadInterfaces() {
            try {
                const data = await getInterfaces();
                setInterfaces(data);
                if (data.length > 0) {
                    setActiveInterface(data[0].name);
                    setExpandedInterface(data[0].name);
                }
            } catch (error) {
                console.error("Klarte ikke å hente WiFi-grensesnitt:", error);
            } finally {
                setIsLoadingInterfaces(false);
            }
        }
        loadInterfaces();
    }, []);

    const toggleShowHidden = (interfaceName: string) => {
        setShowHiddenMap(prev => ({
            ...prev,
            [interfaceName]: !prev[interfaceName]
        }));
    };

    return (
        <Box sx={{ maxWidth: "800px", mx: "auto", p: 4, display: "flex", flexDirection: "column", gap: 3 }}>
            <Typography variant="h5" sx={{ fontWeight: 600 }}>Wi-Fi Settings</Typography>

            {interfaces.map((iface) => {
                const scanData = wifiScans.find(s => s.name === iface.name);
                const connData = wifiConnections.find(c => c.name === iface.name);
                const isExpanded = expandedInterface === iface.name;
                const showHidden = !!showHiddenMap[iface.name];

                return (
                    <Box
                        key={iface.name}
                        sx={{
                            bgcolor: 'grey.900',
                            border: 1,
                            borderColor: 'grey.900',
                            borderRadius: 2.5,
                            overflow: 'hidden'
                        }}
                    >
                        {/* Grensesnitt-header med ekspandering og plass til knapper ved siden av */}
                        <Box
                            sx={{
                                p: 2,
                                display: "flex",
                                alignItems: "center",
                                borderBottom: isExpanded ? "1px solid rgba(255,255,255,0.05)" : "none"
                            }}
                        >
                            <Box sx={{ display: "flex", alignItems: "center", flexGrow: 1 }}>
                                <Typography sx={{ fontWeight: 600, flexGrow: 1 }}>{iface.name}</Typography>

                                <Tooltip title={showHidden ? "Skjul skjulte nettverk" : "Vis skjulte nettverk"} arrow>
                                    <Box component="span" sx={{ display: 'inline-flex' }}>
                                        <VisibilityIcon
                                            visible={showHidden}
                                            onChange={() => toggleShowHidden(iface.name)}
                                            sx={{ ml: 1, mr: 1 }}
                                        />
                                    </Box>
                                </Tooltip>

                                <Tooltip title={isExpanded ? "Lukk" : "Åpne"} arrow>
                                    <IconButton onClick={() => setExpandedInterface(prev => prev === iface.name ? false : iface.name)}>
                                        <ExpandMoreIcon
                                            sx={{
                                                transform: isExpanded ? "rotate(180deg)" : "rotate(0deg)",
                                                transition: "transform 0.2s ease-in-out",
                                                color: "text.secondary"
                                            }}
                                        />
                                    </IconButton>
                                </Tooltip>
                            </Box>
                        </Box>

                        {/* Aninert innhold for grensesnittet */}
                        <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                            <Box sx={{ p: 2 }}>
                                <WifiInterfaceContent
                                    scanData={scanData}
                                    connData={connData}
                                    interfaceName={iface.name}
                                    isExpanded={isExpanded}
                                    showHidden={showHidden}
                                />
                            </Box>
                        </Collapse>
                    </Box>
                );
            })}

            {interfaces.length === 0 && !isLoadingInterfaces && (
                <Box sx={{
                    display: "flex",
                    flexDirection: 'column',
                    alignItems: 'center'
                }}>
                    <WifiOffIcon sx={{
                        mt: 10,
                        fontSize: 72
                    }} />
                    <Typography sx={{ mt: 5 }}>No interfaces found</Typography>
                </Box>
            )}
        </Box>
    );
}

interface WifiNetworkConnectionState {
    bssid: string,
    state: InterfaceActiveState
}

interface WifiInterfaceContentProps {
    scanData: WifiNetworkScan | undefined;
    connData: WifiNetworkConnection | undefined;
    interfaceName: string;
    isExpanded: boolean;
    showHidden: boolean;
}

const WifiInterfaceContent = ({ scanData, connData, interfaceName, isExpanded, showHidden }: WifiInterfaceContentProps) => {
    const isScanning = scanData?.state === "Scanning";
    const [connState, setConnState] = useState<WifiNetworkConnectionState | undefined>();
    const [expandedBssid, setExpandedBssid] = useState<string | null>(null);

    useEffect(() => {
        if (!isExpanded) return;
        const interval = setInterval(() => {
            if (scanData?.state !== "Scanning") {
                getNewNetworks(interfaceName);
            }
        }, 30000);
        return () => clearInterval(interval);
    }, [interfaceName, scanData?.state, isExpanded]);

    useEffect(() => {
        const activeNetwork = connData?.network;
        if (activeNetwork) {
            setConnState({
                bssid: activeNetwork.bssid,
                state: connData.state
            });
        } else {
            setConnState(undefined);
        }
    }, [connData, scanData]);

    const connectedNetwork = connData?.network;
    const isConnected = connData?.state === "Connected" && connectedNetwork;

    const sortedNetworks = (scanData?.networks || [])
        .filter((net: WifiNetwork) => {
            if (isConnected && net.bssid === connectedNetwork.bssid) return false;
            if (!showHidden && net.isHidden) return false;
            return true;
        })
        .sort((a, b) => {
            if (expandedBssid) {
                if (a.bssid === expandedBssid) return -1;
                if (b.bssid === expandedBssid) return 1;
            }
            return b.signalPercent - a.signalPercent;
        });

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <ConnectedNetworkSection
                connectedNetwork={connectedNetwork}
                isConnected={!!isConnected}
                connState={connState}
                connData={connData}
                expandedBssid={expandedBssid}
                setExpandedBssid={setExpandedBssid}
                allNetworks={scanData?.networks || []}
            />

            <AvailableNetworksSection
                sortedNetworks={sortedNetworks}
                isScanning={isScanning}
                interfaceName={interfaceName}
                connState={connState}
                expandedBssid={expandedBssid}
                setExpandedBssid={setExpandedBssid}
                allNetworks={scanData?.networks || []}
            />
        </Box>
    );
};

interface ConnectedNetworkSectionProps {
    connectedNetwork: WifiNetwork | undefined | null;
    isConnected: boolean;
    connState: WifiNetworkConnectionState | undefined;
    connData: WifiNetworkConnection | undefined;
    expandedBssid: string | null;
    setExpandedBssid: (bssid: string | null) => void;
    allNetworks: WifiNetwork[];
}

const ConnectedNetworkSection = ({
    connectedNetwork,
    isConnected,
    connState,
    connData,
    expandedBssid,
    setExpandedBssid,
    allNetworks
}: ConnectedNetworkSectionProps) => {
    const hasError = !!connData?.error;
    const targetNetwork = connectedNetwork || connData?.network;

    if ((!isConnected || !targetNetwork) && !hasError) return null;
    if (!targetNetwork) return null;

    const isConnectedCardExpanded = expandedBssid === targetNetwork.bssid;
    const connectedOverlappingFreq = allNetworks.some(
        otherNet => otherNet.ssid === targetNetwork.ssid && otherNet.frequencyMhz !== targetNetwork.frequencyMhz
    );

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <Typography variant="overline" sx={{ color: 'text.secondary', px: 1 }}>
                {hasError ? "Connection Failed" : "Connected Network"}
            </Typography>
            <List disablePadding sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <WifiNetworkCard
                    key={`connected-${targetNetwork.bssid}-${targetNetwork.ssid}`}
                    wifi={targetNetwork}
                    state={connState?.state || connData?.state || "Disconnected"}
                    expanded={isConnectedCardExpanded}
                    onToggle={() => setExpandedBssid(isConnectedCardExpanded ? null : targetNetwork.bssid)}
                    overlappingSSIDFreq={connectedOverlappingFreq}
                />

                {hasError && (
                    <Box sx={{ px: 1 }}>
                        <Typography variant="caption" sx={{ color: 'error.main', fontWeight: 500 }}>
                            Feil passord. Vennligst prøv igjen.
                        </Typography>
                    </Box>
                )}
            </List>
        </Box>
    );
};

interface AvailableNetworksSectionProps {
    sortedNetworks: WifiNetwork[];
    isScanning: boolean;
    interfaceName: string;
    connState: WifiNetworkConnectionState | undefined;
    expandedBssid: string | null;
    setExpandedBssid: (bssid: string | null) => void;
    allNetworks: WifiNetwork[];
}

const AvailableNetworksSection = ({
    sortedNetworks,
    isScanning,
    interfaceName,
    connState,
    expandedBssid,
    setExpandedBssid,
    allNetworks
}: AvailableNetworksSectionProps) => {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 1 }}>
                <Typography variant="overline" sx={{ color: 'text.secondary' }}>Available Networks</Typography>
                <Button
                    size="small"
                    onClick={() => getNewNetworks(interfaceName)}
                    disabled={isScanning}
                    startIcon={isScanning ? <CircularProgress size={14} color="inherit" /> : null}
                >
                    {isScanning ? "Scanning..." : "Scan"}
                </Button>
            </Box>

            <List disablePadding sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {!isScanning && sortedNetworks.length === 0 && (
                    <Box sx={{ textAlign: 'center', p: 3, color: 'text.secondary' }}>
                        <Typography variant="body2">No other networks found.</Typography>
                    </Box>
                )}

                {sortedNetworks.map((net: WifiNetwork) => {
                    const networkState = connState?.bssid === net.bssid ? connState.state : "Idle" as InterfaceActiveState;
                    const isCardExpanded = expandedBssid === net.bssid;

                    const overlappingSSIDFreq = allNetworks.some(
                        otherNet => otherNet.ssid === net.ssid && otherNet.frequencyMhz !== net.frequencyMhz
                    );

                    return (
                        <WifiNetworkCard
                            key={`${net.bssid}-${net.ssid}`}
                            wifi={net}
                            state={networkState}
                            expanded={isCardExpanded}
                            onToggle={() => setExpandedBssid(isCardExpanded ? null : net.bssid)}
                            overlappingSSIDFreq={overlappingSSIDFreq}
                        />
                    );
                })}
            </List>
        </Box>
    );
};