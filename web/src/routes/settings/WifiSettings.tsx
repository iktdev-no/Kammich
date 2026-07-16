import React, { useEffect, useState } from "react";
import {
    Box,
    Typography,
    Button,
    List,
    ListItem,
    ListItemText,
    CircularProgress,
    TextField,
    MenuItem,
    Select,
    FormControl,
    InputLabel,
    Accordion,
    AccordionDetails,
    AccordionSummary
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import RouterIcon from '@mui/icons-material/Router';
import WifiOffIcon from '@mui/icons-material/WifiOff';


import { useSseSelector } from "../../sse/useSseSelector";
import type { WifiInterface, WifiInterfaceScanState, WifiInterfaceState, WifiNetwork } from "../../types/types";
import { connectToWifi, disconnectFromWifi, getWifiInterfaces, startWifiScan } from "../../api/wifiApi"; // Sørg for at stien stemmer overens med ditt prosjekt
import { WifiSignalIcon } from "../../components/wifi/WifiIcon";

export default function WifiSettings() {
    const [activeInterface, setActiveInterface] = useState<string>("");
    const [expandedInterface, setExpandedInterface] = useState<string | false>(false);

    const handleAccordionChange = (panel: string) => (event: React.SyntheticEvent, isExpanded: boolean) => {
        setExpandedInterface(isExpanded ? panel : false);
    };

    // Data fra SSE
    const [interfaces, setInterfaces] = useState<WifiInterface[]>([]);
    const [isLoadingInterfaces, setIsLoadingInterfaces] = useState(true);


    const wifiScans = useSseSelector(state => state.wifiScans) || [];
    const wifiConnections = useSseSelector(state => state.wifiConnections) || [];

    const activeScan = wifiScans.find(s => s.interfaceName === activeInterface);
    const activeConn = wifiConnections.find(c => c.interfaceName === activeInterface);


    useEffect(() => {
        async function loadInterfaces() {
            try {
                const data = await getWifiInterfaces();
                setInterfaces(data);
                if (data.length > 0) {
                    // 1. Sett aktivt grensesnitt
                    setActiveInterface(data[0].name);

                    // 2. Åpne accordeonen automatisk for det første grensesnittet
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

    return (
        <Box sx={{ maxWidth: "800px", mx: "auto", p: 4, display: "flex", flexDirection: "column", gap: 3 }}>
            <Typography variant="h5" sx={{ fontWeight: 600 }}>Wi-Fi Settings</Typography>

            {interfaces.map((iface) => {
                const scanData = wifiScans.find(s => s.interfaceName === iface.name);
                const connData = wifiConnections.find(c => c.interfaceName === iface.name);

                return (
                    <Accordion
                        key={iface.name}
                        expanded={expandedInterface === iface.name}
                        onChange={handleAccordionChange(iface.name)}
                        sx={{ bgcolor: 'grey.950', border: 1, borderColor: 'grey.900' }}
                    >
                        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                            <Typography sx={{ fontWeight: 600 }}>{iface.name}</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <WifiInterfaceContent
                                scanData={scanData}
                                connData={connData}
                                interfaceName={iface.name}
                                // Send med om denne er "aktiv" (åpen)
                                isExpanded={expandedInterface === iface.name}
                            />
                        </AccordionDetails>
                    </Accordion>
                );
            })}
            {interfaces.length === 0 && (
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


const WifiInterfaceContent = ({ scanData, connData, interfaceName, isExpanded }: { scanData: WifiInterfaceScanState | undefined, connData: WifiInterfaceState | undefined, interfaceName: string, isExpanded: boolean }) => {
    const isScanning = scanData?.scanning === "SCANNING";
    const [showHidden, setShowHidden] = useState(false); // 1. Toggle for skjulte

    useEffect(() => {
        if (!isExpanded) return;
        const interval = setInterval(() => {
            if (scanData?.scanning !== "SCANNING") {
                startWifiScan(interfaceName);
            }
        }, 30000);
        return () => clearInterval(interval);
    }, [interfaceName, scanData?.scanning, isExpanded]);

    // 2. Logikk for filtrering
    const visibleNetworks = (scanData?.networks || [])
        .filter((net: WifiNetwork) => {
            // Filtrer ut den vi er tilkoblet
            if (net.ssid === connData?.network?.ssid) return false;
            // Hvis showHidden er false, filtrer bort de skjulte
            if (!showHidden && net.isHidden) return false;
            return true;
        });

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {connData?.connectivityState === "CONNECTED" && connData.network && (
                <Box sx={{ bgcolor: 'rgba(76, 175, 80, 0.1)', p: 2, borderRadius: 1, border: '1px solid rgba(76, 175, 80, 0.3)' }}>
                    <Typography variant="caption" sx={{ color: 'success.main', fontWeight: 600, mb: 1, display: 'block' }}>
                        ACTIVE CONNECTION
                    </Typography>
                    <NetworkListItem network={connData.network} isConnected={true} />
                </Box>
            )}

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 1 }}>
                <Typography variant="overline" sx={{ color: 'text.secondary' }}>Available Networks</Typography>
                <Box>

                    <Button
                        size="small"
                        onClick={() => startWifiScan(interfaceName)}
                        disabled={isScanning}
                        startIcon={isScanning ? <CircularProgress size={14} color="inherit" /> : null}
                    >
                        {isScanning ? "Scanning..." : "Scan"}
                    </Button>
                </Box>

            </Box>

            <List disablePadding>

                {/* 2. Tom-melding: Kun hvis vi IKKE scanner og listen er tom */}
                {!isScanning && visibleNetworks.length === 0 && (
                    <Box sx={{ textAlign: 'center', p: 3, color: 'text.secondary' }}>
                        <Typography variant="body2">No networks found.</Typography>
                        {connData?.connectivityState === "CONNECTED" && (
                            <Typography variant="caption" sx={{ display: 'block', mt: 1 }}>
                                Hint: Some systems restrict scanning while connected.
                            </Typography>
                        )}
                    </Box>
                )}

                {/* 3. Selve listen - denne vises nå ALLTID, også under scanning */}
                {visibleNetworks.map((net: WifiNetwork) => (
                    <NetworkListItem key={`${net.bssid}-${net.ssid}`} network={net} isConnected={false} />
                ))}
            </List>
            <Button size="small" onClick={() => setShowHidden(!showHidden)}>
                {showHidden ? "Hide Hidden" : "Show Hidden"}
            </Button>
        </Box>
    );
};

export const NetworkListItem = ({ network, isConnected = false }: { network: WifiNetwork, isConnected: boolean }) => {
    const [expanded, setExpanded] = useState(false);
    const [password, setPassword] = useState<string | undefined>(undefined);

    return (
        <Box sx={{
            borderBottom: isConnected ? "none" : "1px solid rgba(255,255,255,0.05)",
            bgcolor: isConnected ? 'rgba(76, 175, 80, 0.05)' : (expanded ? 'rgba(0,0,0,0.5)' : 'rgba(255,255,255,0.05)'),
            borderRadius: 1,
            mt: 1, mb: 1
        }}>
            <ListItem
                onClick={() => setExpanded(!expanded)}
                sx={{ cursor: 'pointer', py: 1.5 }}
            >
                <ListItemText
                    primary={network.ssid}
                    secondary={isConnected ? "Connected" : network.securityType}
                    slotProps={{
                        primary: { sx: { fontWeight: isConnected ? 600 : 500 } }
                    }}
                />

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    {!isConnected && <Typography variant="caption" sx={{ color: 'text.secondary' }}>{network.signalPercent}%</Typography>}
                    <WifiSignalIcon isSecure={network.isSecure} strength={network.signalPercent} />
                </Box>
            </ListItem>

            {/* Expanderbar seksjon */}
            {expanded && (
                <Box sx={{ p: 2, display: 'flex', gap: 1, bgcolor: 'rgba(0,0,0,0.5)', borderRadius: 1, mr: 1, ml: 1, mb: 1 }}>
                    {isConnected ? (
                        <Button
                            variant="outlined"
                            color="error"
                            size="small"
                            fullWidth
                            onClick={() => disconnectFromWifi(network.interfaceName)}
                        >
                            Disconnect
                        </Button>
                    ) : (
                        <>
                            {network.isSecure && (
                                <TextField size="small" label="Password" type="password" fullWidth value={password} onChange={(e) => setPassword(e.target.value)} />
                            )}
                            <Button variant="contained" size="small" onClick={() => {
                                connectToWifi(network.interfaceName, network.bssid, password)
                            }} sx={{ textTransform: 'none', px: 3 }}>Connect</Button>
                        </>
                    )}
                </Box>
            )}
        </Box>
    );
};

export const WifiInterfaceSelector = ({
    interfaces, activeInterface, onChange, connStatus
}: any) => (
    <Box sx={{ display: "flex", alignItems: "center", gap: 2, p: 2, bgcolor: "grey.900", borderRadius: 1, border: 1, borderColor: "grey.800" }}>
        <RouterIcon sx={{ color: "text.secondary" }} />
        <FormControl variant="standard" sx={{ flexGrow: 1 }}>
            <InputLabel>Active Interface</InputLabel>
            <Select value={activeInterface} onChange={(e) => onChange(e.target.value)}>
                {interfaces.map((i: any) => (
                    <MenuItem key={i.name} value={i.name}>{i.name} {i.supportsAp ? "(AP)" : ""}</MenuItem>
                ))}
            </Select>
        </FormControl>
        <Typography variant="caption" sx={{ color: connStatus === "CONNECTED" ? "success.main" : "text.secondary" }}>
            {connStatus}
        </Typography>
    </Box>
);
