import React, { useEffect, useState } from "react";
import {
    Box,
    Typography,
    Button,
    List,
    ListItem,
    ListItemText,
    ListItemIcon,
    CircularProgress,
    TextField,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    useTheme,
    MenuItem,
    Select,
    FormControl,
    InputLabel
} from "@mui/material";
import WifiIcon from '@mui/icons-material/Wifi';
import WifiPasswordIcon from '@mui/icons-material/WifiPassword';
import SignalCellularAltIcon from "@mui/icons-material/SignalCellularAlt";
import WifiFindOutlinedIcon from '@mui/icons-material/WifiFindOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import RouterIcon from '@mui/icons-material/Router';
import GraphicEqIcon from '@mui/icons-material/GraphicEq';

import { useSseSelector } from "../../sse/useSseSelector";
import type { WifiNetwork, WifiInterface } from "../../types/types";
import { getWifiInterfaces, startWifiScan } from "../../api/wifiApi"; // Sørg for at stien stemmer overens med ditt prosjekt

export default function WifiSettings() {
    const theme = useTheme();

    // Henter sanntidsdata og statuser direkte fra din sentrale SSE-state
    
    const scanStatus = useSseSelector(state => state.wifiScanState) || "IDLE";
    const networks = useSseSelector(state => state.wifiNetworks) || [];

    // Tilstander for WiFi-grensesnitt (interfaces)
    const [interfaces, setInterfaces] = useState<WifiInterface[]>([]);
    const [activeInterface, setActiveInterface] = useState<string>("");
    const [isLoadingInterfaces, setIsLoadingInterfaces] = useState(true);

    // Lokale tilstander for dialogboksen ved tilkobling
    const [selectedNetwork, setSelectedNetwork] = useState<FeWifiNetwork | null>(null);
    const [password, setPassword] = useState("");
    const [isConnectingLocally, setIsConnectingLocally] = useState(false);

    // 1. Hent tilgjengelige grensesnitt når komponenten mountes
    useEffect(() => {
        async function loadInterfaces() {
            try {
                const data = await getWifiInterfaces();
                setInterfaces(data);
                if (data.length > 0) {
                    // Setter det første tilgjengelige grensesnittet som standard (f.eks. wlan0)
                    setActiveInterface(data[0].name);
                }
            } catch (error) {
                console.error("Klarte ikke å hente WiFi-grensesnitt:", error);
            } finally {
                setIsLoadingInterfaces(false);
            }
        }
        loadInterfaces();
    }, []);

    // 2. Trigger en re-scan asynkront basert på det aktive kortet
    const handleRefresh = async () => {
        if (!activeInterface) return;
        
        try {
            await startWifiScan(activeInterface);
        } catch (error) {
            console.error(`Klarte ikke å initiere WiFi-skanning på ${activeInterface}:`, error);
        }
    };

    // 3. Kjører en automatisk skanning så fort grensesnittet er identifisert og klart
    useEffect(() => {
        if (activeInterface) {
            handleRefresh();
        }
    }, [activeInterface]);

    const handleNetworkClick = (network: FeWifiNetwork) => {
        if (network.isSecure) {
            setSelectedNetwork(network);
            setPassword("");
        } else {
            handleConnect(network, "");
        }
    };

    const handleConnect = async (network: FeWifiNetwork, pass: string) => {
        setIsConnectingLocally(true);
        try {
            // OBS: Hvis dere har en apiPost-wrapper for connect også, bør den byttes ut her etter hvert.
            const response = await fetch("/api/v1/wifi/connect", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ ssid: network.ssid, bssid: network.bssid, password: pass })
            });
            const result = await response.json();

            if (!result.success) {
                alert(`Kunne ikke koble til: ${result.message}`);
            }
        } catch (error) {
            console.error("Nettverksfeil ved oppkobling:", error);
        } finally {
            setIsConnectingLocally(false);
            setSelectedNetwork(null);
        }
    };

    if (isLoadingInterfaces) {
        return (
            <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "200px" }}>
                <CircularProgress size={32} />
            </Box>
        );
    }

    return (
        <Box
            sx={{
                flexGrow: 1,
                backgroundColor: theme.palette.background.default,
                color: theme.palette.text.primary,
                padding: theme.spacing(4),
                maxWidth: "800px",
                display: "flex",
                flexDirection: "column",
                gap: theme.spacing(3),
            }}
        >
            {/* Header i flatt Immich-design */}
            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: `1px solid ${theme.palette.grey[800]}`, pb: 2 }}>
                <Box>
                    <Typography variant="h5" sx={{ fontWeight: 600, letterSpacing: "-0.5px" }}>
                        Wi-Fi Settings
                    </Typography>
                    <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
                        Manage network connections for your Kammich node.
                    </Typography>
                </Box>

                <Button
                    variant="outlined"
                    startIcon={scanStatus === "SCANNING" ? <CircularProgress size={16} color="inherit" /> : <WifiFindOutlinedIcon />}
                    onClick={handleRefresh}
                    disabled={scanStatus === "SCANNING" || status === "CONNECTING" || !activeInterface}
                    sx={{
                        borderColor: theme.palette.grey[700],
                        color: theme.palette.text.primary,
                        textTransform: "none",
                        fontWeight: 500,
                        "&:hover": { borderColor: theme.palette.text.primary, backgroundColor: "transparent" }
                    }}
                >
                    {scanStatus === "SCANNING" ? "Scanning..." : "Scan networks"}
                </Button>
            </Box>

            {/* Grensesnitt-velger (vises kun hvis maskinen har mer enn 1 kort, f.eks. wlan0 og wlan1) */}
            {interfaces.length > 0 && (
                <Box sx={{ display: "flex", alignItems: "center", gap: 2, p: 2, backgroundColor: theme.palette.grey[900], borderRadius: "4px", border: `1px solid ${theme.palette.grey[800]}` }}>
                    <RouterIcon sx={{ color: theme.palette.text.secondary }} />
                    <FormControl variant="standard" sx={{ minWidth: 160 }}>
                        <InputLabel id="interface-select-label" sx={{ color: theme.palette.text.secondary, fontSize: "12px" }}>Active Interface</InputLabel>
                        <Select
                            labelId="interface-select-label"
                            value={activeInterface}
                            onChange={(e) => setActiveInterface(e.target.value)}
                            disabled={status === "SCANNING" || status === "CONNECTING"}
                            sx={{ color: theme.palette.text.primary, fontSize: "14px", pt: 1 }}
                        >
                            {interfaces.map((iface) => (
                                <MenuItem key={iface.name} value={iface.name}>
                                    {iface.name} {iface.supportsAp ? "(AP Capable)" : ""}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Box>
            )}

            {/* Status-banner hvis enheten holder på å koble seg til et nettverk */}
            {(status === "CONNECTING" || isConnectingLocally) && (
                <Box sx={{ display: "flex", alignItems: "center", gap: 2, p: 2, backgroundColor: theme.palette.grey[900], borderRadius: "4px", border: `1px solid ${theme.palette.grey[800]}` }}>
                    <CircularProgress size={20} thickness={5} />
                    <Typography variant="body2">Connecting to network, establishing handshake...</Typography>
                </Box>
            )}

            {/* Status-banner ved systemfeil */}
            {status === "ERROR" && (
                <Box sx={{ display: "flex", alignItems: "center", gap: 2, p: 2, backgroundColor: "rgba(239, 68, 68, 0.1)", borderRadius: "4px", border: "1px solid rgb(239, 68, 68)" }}>
                    <ErrorOutlineOutlinedIcon sx={{ color: "rgb(239, 68, 68)" }} />
                    <Typography variant="body2" sx={{ color: "rgb(239, 68, 68)" }}>An error occurred while communicating with the wireless interface.</Typography>
                </Box>
            )}

            {/* Nettverkslisten */}
            <Box>
                <Typography variant="overline" sx={{ color: theme.palette.text.secondary, fontWeight: 700, letterSpacing: "1px" }}>
                    Available Networks ({networks.length})
                </Typography>

                <List sx={{ mt: 1, display: "flex", flexDirection: "column", gap: "2px" }}>
                    {networks.map((network) => (
                        <ListItem
                            key={network.bssid}
                            onClick={() => (status !== "SCANNING" && status !== "CONNECTING") && handleNetworkClick(network)}
                            component="div"
                            sx={{
                                border: `1px solid ${theme.palette.grey[900]}`,
                                borderBottom: `1px solid ${theme.palette.grey[800]}`,
                                backgroundColor: "transparent",
                                cursor: (status === "SCANNING" || status === "CONNECTING") ? "default" : "pointer",
                                transition: "all 0.15s ease",
                                "&:hover": {
                                    backgroundColor: (status === "SCANNING" || status === "CONNECTING") ? "transparent" : theme.palette.grey[900],
                                    borderColor: (status === "SCANNING" || status === "CONNECTING") ? theme.palette.grey[800] : theme.palette.grey[700],
                                },
                                py: 1.5,
                                px: 2,
                                display: "flex",
                                alignItems: "center"
                            }}
                        >
                            <ListItemIcon sx={{ minWidth: "40px", color: theme.palette.text.primary }}>
                                {network.isSecure ? <WifiPasswordIcon sx={{ mr: 2, fontSize: 20 }} /> : <WifiIcon sx={{ mr: 2, fontSize: 20 }} />}
                            </ListItemIcon>

                            <ListItemText
                                primary={network.ssid}
                                secondary={network.securityType}
                                slotProps={{
                                    primary: { style: { fontWeight: 500, fontSize: "14px" } },
                                    secondary: { style: { fontSize: "12px", color: theme.palette?.text?.secondary || "#888" } }
                                }}
                            />

                            <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                                <Typography variant="body2" sx={{ color: theme.palette.text.secondary, fontSize: "13px" }}>
                                    {network.signalPercent}%
                                </Typography>
                                <SignalCellularAltIcon
                                    sx={{
                                        color: network.signalPercent > 70 ? "success.main" : network.signalPercent > 40 ? "warning.main" : "error.main",
                                        opacity: 0.8
                                    }}
                                />
                            </Box>
                        </ListItem>
                    ))}

                    {networks.length === 0 && status !== "SCANNING" && (
                        <Box sx={{ textAlign: "center", py: 6, border: `1px dashed ${theme.palette.grey[800]}`, borderRadius: "4px" }}>
                            <Typography variant="body2" sx={{ color: theme.palette.text.secondary }}>
                                {activeInterface ? "No networks found. Ensure your Wi-Fi antenna is connected." : "No active Wi-Fi interfaces found on this node."}
                            </Typography>
                        </Box>
                    )}
                </List>
            </Box>

            {/* Autentiserings-dialog for passordbeskyttede nettverk */}
            <Dialog
                open={selectedNetwork !== null}
                onClose={() => !isConnectingLocally && setSelectedNetwork(null)}
                slotProps={{
                    paper: {
                        sx: {
                            backgroundColor: theme.palette.grey[900] || "#0a0a0a",
                            backgroundImage: "none",
                            border: `1px solid ${theme.palette.grey[800]}`,
                            borderRadius: "4px",
                            minWidth: "320px"
                        }
                    }
                }}
            >
                <DialogTitle sx={{ fontSize: "16px", fontWeight: 600, pb: 1 }}>
                    Connect to {selectedNetwork?.ssid}
                </DialogTitle>
                <DialogContent sx={{ pt: 1 }}>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="Password"
                        type="password"
                        fullWidth
                        variant="outlined"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        disabled={isConnectingLocally}
                        slotProps={{ inputLabel: { style: { color: theme.palette.text.secondary } } }}
                        sx={{
                            "& .MuiOutlinedInput-root": {
                                "& fieldset": { borderColor: theme.palette.grey[700] },
                                "&:hover fieldset": { borderColor: theme.palette.text.primary },
                                "&.Mui-focused fieldset": { borderColor: theme.palette.primary.main }
                            }
                        }}
                    />
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button
                        onClick={() => setSelectedNetwork(null)}
                        disabled={isConnectingLocally}
                        sx={{ color: theme.palette.text.secondary, textTransform: "none" }}
                    >
                        Cancel
                    </Button>
                    <Button
                        onClick={() => selectedNetwork && handleConnect(selectedNetwork, password)}
                        variant="contained"
                        disabled={isConnectingLocally || !password}
                        sx={{ textTransform: "none", boxShadow: "none", fontWeight: 500 }}
                    >
                        Connect
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}