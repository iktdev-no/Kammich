import { useEffect, useState } from "react";
import {
    Box,
    Typography,
    Button,
    List,
    CircularProgress,
    Collapse,
    IconButton,
    Tooltip
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import WifiOffIcon from '@mui/icons-material/WifiOff';
import WifiTetheringIcon from '@mui/icons-material/WifiTethering';

import { useSseSelector } from "../../sse/useSseSelector";
import type { NetworkInterfaceMode, WifiConnectionStateType, WifiInterfaceClient, WifiNetwork, WifiConnection } from "../../types/types";
import { getInterfaces, getNetworks, startNetworkScan, stopNetworkScan } from "../../api/requests/networking/connection";
import WifiNetworkCard from "../../components/network/WifiNetwork";
import { VisibilityIcon } from "../../components/icons/VisibilityIcon";

export default function WifiSettings() {
    const [expandedInterface, setExpandedInterface] = useState<string | false>(false);
    const [showHiddenMap, setShowHiddenMap] = useState<Record<string, boolean>>({});

    const [isLoadingInterfaces, setIsLoadingInterfaces] = useState(true);

    // Henter lister og tilstander fra SSE basert på den nye Record/Array-strukturen
    const interfacesFromSse = useSseSelector(state => state.wifiConnectionInterfaces);
    const [localInterfaces, setLocalInterfaces] = useState<WifiInterfaceClient[]>([]);

    const wifiScanStatuses = useSseSelector(state => state.wifiScanStatuses) || {};
    const wifiScanResults = useSseSelector(state => state.wifiScanResults) || {};
    const wifiConnections = useSseSelector(state => state.wifiConnection) || {};

    // Initial load / fallback via API hvis SSE ikke har dumpet noe ennå
    useEffect(() => {
        async function loadInterfaces() {
            try {
                const data = await getInterfaces();
                setLocalInterfaces(data);
                if (data.length > 0) {
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

    // Oppdater lokal state så fort SSE dumper en ny liste (`wifi-interface-client`)
    useEffect(() => {
        if (interfacesFromSse && interfacesFromSse.length > 0) {
            setLocalInterfaces(interfacesFromSse);
            setIsLoadingInterfaces(false);

            // Sett første som ekspandert som standard om ingen er valgt
            setExpandedInterface(prev => prev === false && interfacesFromSse.length > 0 ? interfacesFromSse[0].name : prev);
        }
    }, [interfacesFromSse]);

    const toggleShowHidden = (interfaceName: string) => {
        setShowHiddenMap(prev => ({
            ...prev,
            [interfaceName]: !prev[interfaceName]
        }));
    };

    return (
        <Box sx={{ maxWidth: "800px", mx: "auto", p: 4, display: "flex", flexDirection: "column", gap: 3 }}>
            <Typography variant="h5" sx={{ fontWeight: 600 }}>Wi-Fi Settings</Typography>

            {localInterfaces.map((iface) => {
                const name = iface.name;
                const scanStatus = wifiScanStatuses[name];
                const scanResult = wifiScanResults[name];
                const wifiConn = wifiConnections[name]; // Henter WifiConnection-objektet for dette interfacet

                const isExpanded = expandedInterface === name;
                const showHidden = !!showHiddenMap[name];

                return (
                    <Box
                        key={name}
                        sx={{
                            bgcolor: 'grey.900',
                            border: 1,
                            borderColor: 'grey.900',
                            borderRadius: 2.5,
                            overflow: 'hidden'
                        }}
                    >
                        <Box
                            sx={{
                                p: 2,
                                display: "flex",
                                alignItems: "center",
                                borderBottom: isExpanded ? "1px solid rgba(255,255,255,0.05)" : "none"
                            }}
                        >
                            <Box sx={{ display: "flex", alignItems: "center", flexGrow: 1 }}>
                                <Typography sx={{ fontWeight: 600, flexGrow: 1 }}>
                                    {name}
                                    {!iface.isUsable && (
                                        <Typography component="span" variant="caption" sx={{ ml: 2, color: 'text.secondary' }}>
                                            (In use: {iface.operatingMode})
                                        </Typography>
                                    )}
                                </Typography>

                                {iface.isUsable && (
                                    <Tooltip title={showHidden ? "Skjul skjulte nettverk" : "Vis skjulte nettverk"} arrow>
                                        <Box component="span" sx={{ display: 'inline-flex' }}>
                                            <VisibilityIcon
                                                visible={showHidden}
                                                onChange={() => toggleShowHidden(name)}
                                                sx={{ ml: 1, mr: 1 }}
                                            />
                                        </Box>
                                    </Tooltip>
                                )}

                                <Tooltip title={isExpanded ? "Lukk" : "Åpne"} arrow>
                                    <IconButton onClick={() => setExpandedInterface(prev => prev === name ? false : name)}>
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

                        <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                            <Box sx={{ p: 2 }}>
                                <WifiInterfaceContent
                                    isUsable={iface.isUsable}
                                    operatingMode={iface.operatingMode}
                                    scanStatus={scanStatus}
                                    scanResult={scanResult}
                                    wifiConn={wifiConn}
                                    interfaceName={name}
                                    isExpanded={isExpanded}
                                    showHidden={showHidden}
                                />
                            </Box>
                        </Collapse>
                    </Box>
                );
            })}

            {localInterfaces.length === 0 && !isLoadingInterfaces && (
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

interface WifiInterfaceContentProps {
    isUsable: boolean;
    operatingMode: NetworkInterfaceMode;
    scanStatus?: { isScanning: boolean };
    scanResult?: { networks: WifiNetwork[] };
    wifiConn?: WifiConnection;
    interfaceName: string;
    isExpanded: boolean;
    showHidden: boolean;
}

const WifiInterfaceContent = ({
    isUsable,
    operatingMode,
    scanStatus,
    scanResult,
    wifiConn,
    interfaceName,
    isExpanded,
    showHidden
}: WifiInterfaceContentProps) => {
    const isScanning = !!scanStatus?.isScanning;
    const [expandedBssid, setExpandedBssid] = useState<string | null>(null);

    const [displayedNetworks, setDisplayedNetworks] = useState<WifiNetwork[]>([]);

    useEffect(() => {
        if (expandedBssid === null && scanResult?.networks) {
            setDisplayedNetworks(scanResult.networks);
        }
    }, [scanResult, expandedBssid]);

    // Starter periodisk skanning ved mount / ekspandering, og stopper ved unmount / lukking
    useEffect(() => {
        if (!isExpanded || !isUsable) return;

        startNetworkScan(interfaceName);

        return () => {
            stopNetworkScan(interfaceName);
        };
    }, [interfaceName, isExpanded, isUsable]);

    if (!isUsable) {
        return (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', p: 3, gap: 1.5, color: 'text.secondary' }}>
                <WifiTetheringIcon sx={{ fontSize: 40 }} />
                <Typography variant="body2" sx={{ textAlign: 'center' }}>
                    Interface is currently unavailable because it is operating in <strong>{operatingMode}</strong> mode.
                </Typography>
            </Box>
        );
    }

    const connectedNetwork = wifiConn?.network;
    const isConnected = wifiConn?.state === "Connected" && connectedNetwork;

    const sortedNetworks = displayedNetworks
        .filter((net: WifiNetwork) => {
            if (isConnected && connectedNetwork && net.bssid === connectedNetwork.bssid) return false;
            if (!showHidden && net.isHidden) return false;
            return true;
        })
        .sort((a, b) => b.signalPercent - a.signalPercent);

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <ConnectedNetworkSection
                connectedNetwork={connectedNetwork}
                isConnected={!!isConnected}
                wifiConn={wifiConn}
                expandedBssid={expandedBssid}
                setExpandedBssid={setExpandedBssid}
                allNetworks={displayedNetworks}
            />

            <AvailableNetworksSection
                sortedNetworks={sortedNetworks}
                isScanning={isScanning}
                interfaceName={interfaceName}
                wifiConn={wifiConn}
                expandedBssid={expandedBssid}
                setExpandedBssid={setExpandedBssid}
                allNetworks={displayedNetworks}
            />
        </Box>
    );
};

interface ConnectedNetworkSectionProps {
    connectedNetwork: WifiNetwork | undefined | null;
    isConnected: boolean;
    wifiConn?: WifiConnection;
    expandedBssid: string | null;
    setExpandedBssid: (bssid: string | null) => void;
    allNetworks: WifiNetwork[];
}

const ConnectedNetworkSection = ({
    connectedNetwork,
    isConnected,
    wifiConn,
    expandedBssid,
    setExpandedBssid,
    allNetworks
}: ConnectedNetworkSectionProps) => {
    const hasError = !!wifiConn?.error;
    const targetNetwork = connectedNetwork || wifiConn?.network;

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
                    state={wifiConn?.state || "Disconnected"}
                    expanded={isConnectedCardExpanded}
                    onToggle={() => setExpandedBssid(isConnectedCardExpanded ? null : targetNetwork.bssid)}
                    overlappingSSIDFreq={connectedOverlappingFreq}
                />

                {hasError && (
                    <Box sx={{ px: 1 }}>
                        <Typography variant="caption" sx={{ color: 'error.main', fontWeight: 500 }}>
                            {wifiConn?.error}
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
    wifiConn?: WifiConnection;
    expandedBssid: string | null;
    setExpandedBssid: (bssid: string | null) => void;
    allNetworks: WifiNetwork[];
}

const AvailableNetworksSection = ({
    sortedNetworks,
    isScanning,
    interfaceName,
    wifiConn,
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
                    onClick={() => getNetworks(interfaceName)}
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
                    const networkState = (wifiConn?.network?.bssid === net.bssid ? wifiConn.state : "Idle") as WifiConnectionStateType;
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