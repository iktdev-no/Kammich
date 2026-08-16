import { useEffect, useState } from "react";
import {
    Box,
    Typography,
    Button,
    TextField,
    Paper,
    Chip,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Divider,
    InputAdornment,
    IconButton,
    ListItem,
    ListItemText,
    List,
    useTheme
} from "@mui/material";

import WifiTetheringIcon from '@mui/icons-material/WifiTethering';
import WifiTetheringErrorIcon from '@mui/icons-material/WifiTetheringError';
import WifiIcon from '@mui/icons-material/Wifi';
import WifiOffIcon from '@mui/icons-material/WifiOff';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import WifiTetheringOff from '@mui/icons-material/WifiTetheringOff';

import { useSseSelector } from "../../sse/useSseSelector";
import type { WifiTetherAP, WifiSecurityType, WifiInterfaceTether, WirelessTetheringState, WifiTether } from "../../types/types";
import { getAp, getInterfaces, setAp, startTethering, stopTethering, useTetherDevice, removeTetherDevice } from "../../api/requests/networking/tethering";

export default function WifiApSettings() {
    const theme = useTheme();

    // Henter inn wifiTether Record fra SSE
    const wifiTether = useSseSelector(state => state.wifiTether) || {};

    const [interfaces, setInterfaces] = useState<WifiInterfaceTether[]>([]);
    const [apSetting, setApSetting] = useState<WifiTetherAP | undefined>();

    const getApConfig = async () => {
        const data = await getAp();
        setApSetting(data);
    };

    const updateApSettings = async (tether: WifiTetherAP) => {
        await setAp(tether);
        setApSetting(tether);
    };

    useEffect(() => {
        getInterfaces().then((data) => {
            setInterfaces(data);
        });
        getApConfig();
    }, []);

    // Det aktive grensesnittet er det som ligger registrert i wifiTether-recorden fra backend
    const activeInterfaceName = Object.keys(wifiTether)[0] || null;

    // Hent WifiTether-objektet knyttet spesifikt til det aktive grensesnittet
    const activeTetherObj = activeInterfaceName ? wifiTether[activeInterfaceName] : undefined;
    const activeState: WirelessTetheringState = activeTetherObj?.state || "Idle";

    return (
        <Box sx={{ maxWidth: "1000px", mx: "auto", p: 4 }}>
            <Typography variant="h5" sx={{ fontWeight: 600, mb: 4, display: "flex", alignItems: "center", gap: 1 }}>
                <WifiTetheringIcon /> Access Point Settings
            </Typography>

            <Box>
                <Paper sx={{ p: 3, backgroundColor: theme.palette.background.paper, borderRadius: theme.shape.borderRadius, height: '100%' }}>
                    <Typography variant="h6" sx={{ mb: 2 }}>System Status</Typography>
                    {activeInterfaceName ? (
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                            <Typography>Interface: {activeInterfaceName}</Typography>
                            <Box sx={{ display: "flex", alignItems: "center", flexDirection: "row", gap: 1 }}>
                                <Typography>State:</Typography>
                                <Chip
                                    label={activeState}
                                    size="small"
                                    color={activeState === "Tethering" ? 'success' : activeState === "Acquired" ? 'info' : 'warning'}
                                />
                            </Box>
                            <Divider sx={{ my: 1 }} />
                            {activeTetherObj?.network && (
                                <>
                                    <Typography variant="body2">SSID: {activeTetherObj.network.ssid}</Typography>
                                    <Typography variant="body2">Freq: {activeTetherObj.network.frequencyMhz} MHz</Typography>
                                </>
                            )}
                            <Box sx={{ mt: 1, display: 'flex', gap: 1 }}>
                                {activeState === "Idle" || activeState === "Acquired" ? (
                                    <>
                                        <Button
                                            variant="contained"
                                            size="small"
                                            onClick={() => startTethering(activeInterfaceName)}
                                        >
                                            Start
                                        </Button>
                                        <Button
                                            variant="contained"
                                            color="error"
                                            size="small"
                                            onClick={() => removeTetherDevice(activeInterfaceName)}
                                        >
                                            Remove
                                        </Button>
                                    </>
                                ) : (
                                    <Button
                                        variant="contained"
                                        color="error"
                                        size="small"
                                        onClick={() => stopTethering(activeInterfaceName)}
                                    >
                                        Stop
                                    </Button>
                                )}
                            </Box>
                        </Box>
                    ) : (
                        <Box sx={{
                            display: "flex",
                            flexDirection: "column",
                            justifyContent: "center",
                            alignItems: "center",
                            p: 5
                        }}>
                            <WifiTetheringOff />
                            <Typography sx={{ mt: 1 }}>No interface configured</Typography>
                        </Box>
                    )}
                </Paper>
            </Box>

            <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'grid', gridTemplateColumns: { md: '1fr 1fr' }, gap: 4 }}>
                    <ApTetherConfig config={apSetting} onUpdate={updateApSettings} />
                    <Box sx={{
                        backgroundColor: theme.palette.background.paper,
                        p: 1,
                        borderRadius: theme.shape.borderRadius
                    }}>
                        <AvailableInterfaces
                            availableInterfaces={interfaces}
                            activeInterfaceName={activeInterfaceName}
                            wifiTether={wifiTether}
                        />
                    </Box>
                </Box>
            </Box>
        </Box>
    );
}

export function ActiveTetherDevice({ activeInterfaceName, activeState }: { activeInterfaceName: string | null, activeState: WirelessTetheringState }) {
    return (
        <Box>
            {activeInterfaceName && (
                <InterfaceItem
                    iface={activeInterfaceName}
                    supportsTethering={true}
                    isInUse={true}
                    state={activeState}
                />
            )}
        </Box>
    );
}

function ApTetherConfig({ config, onUpdate }: { config: WifiTetherAP | undefined, onUpdate: (data: WifiTetherAP) => void }) {
    const theme = useTheme();
    const [ssid, setSsid] = useState<string | undefined>();
    const [password, setPassword] = useState<string | undefined>();
    const [security, setSecurity] = useState<WifiSecurityType>('NONE');
    const [showPassword, setShowPassword] = useState<boolean>(false);

    useEffect(() => {
        if (config) {
            setSsid(config.ssid);
            setPassword(config.password);
            setSecurity(config.security);
        }
    }, [config]);

    const SECURITY_OPTIONS: { label: string; value: WifiSecurityType }[] = [
        { label: "None", value: "NONE" },
        { label: "WPA2 Personal", value: "WPA2" },
        { label: "WPA3 Personal", value: "WPA3" },
    ];

    return (
        <Box sx={{
            backgroundColor: theme.palette.background.paper,
            p: 1,
            borderRadius: theme.shape.borderRadius
        }}>
            <Typography sx={{ mt: 2, ml: 2, mb: 2 }} variant="h6">Wifi Network settings</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2 }}>
                <TextField
                    size="small"
                    label="SSID"
                    value={ssid || ""}
                    onChange={(e) => setSsid(e.target.value)}
                    slotProps={{
                        inputLabel: { shrink: !!ssid }
                    }}
                />

                <TextField
                    size="small"
                    label="Password"
                    type={showPassword ? 'text' : 'password'}
                    value={password || ""}
                    onChange={(e) => setPassword(e.target.value)}
                    slotProps={{
                        input: {
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        onClick={() => setShowPassword(!showPassword)}
                                        edge="end"
                                    >
                                        {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                    </IconButton>
                                </InputAdornment>
                            )
                        },
                        inputLabel: { shrink: !!password }
                    }}
                />

                <FormControl fullWidth size="small">
                    <InputLabel id="security-label">Security</InputLabel>
                    <Select
                        labelId="security-label"
                        value={security}
                        label="Security"
                        onChange={(e) => setSecurity(e.target.value as WifiSecurityType)}
                    >
                        {SECURITY_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>

                <Button
                    variant="contained"
                    color="primary"
                    onClick={() => onUpdate({
                        password: password || "",
                        security: security,
                        ssid: ssid || ""
                    })}
                    disabled={!ssid}
                >
                    Save Configuration
                </Button>
            </Box>
        </Box>
    );
}

interface AvailableInterfacesProps {
    availableInterfaces: Array<WifiInterfaceTether>;
    activeInterfaceName: string | null;
    wifiTether: Record<string, WifiTether>;
}

function AvailableInterfaces({ availableInterfaces, activeInterfaceName, wifiTether }: AvailableInterfacesProps) {
    const [interfaces, setInterfaces] = useState<Array<WifiInterfaceTether>>([]);

    useEffect(() => {
        setInterfaces(availableInterfaces);
    }, [availableInterfaces]);

    // Vis enheter som verken er valgt/aktive ELLER har en operativ modus (bortsett fra "Idle" / tom)
    const filteredInterfaces = interfaces.filter(iface => {
        const isSelected = iface.name === activeInterfaceName;
        const isInWifiTether = !!wifiTether[iface.name];

        // En enhet er ledig hvis den ikke er i wifiTether, ikke er valgt, 
        // og enten mangler operatingMode eller står som "Idle"
        const isIdleOrNone = !iface.operatingMode || iface.operatingMode === "Idle";

        return !isSelected && !isInWifiTether && isIdleOrNone;
    });

    return (
        <>
            {filteredInterfaces.length > 0 ? (
                <List disablePadding>
                    {filteredInterfaces.map((iface: WifiInterfaceTether, i: number) => {
                        const supportsAP = iface.caps?.includes("AP") || false;
                        const interfaceState = wifiTether[iface.name]?.state || iface.state || "Idle";
                        const isInUse = iface.name === activeInterfaceName;

                        return (
                            <InterfaceItem
                                key={i}
                                iface={iface.name}
                                supportsTethering={supportsAP}
                                isInUse={isInUse}
                                isUsable={iface.isUsable}
                                operatingMode={iface.operatingMode}
                                state={interfaceState}
                            />
                        );
                    })}
                </List>
            ) : (
                <Box sx={{
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    p: 5
                }}>
                    <WifiTetheringErrorIcon />
                    <Typography sx={{ mt: 1 }}>No available interface</Typography>
                </Box>
            )}
        </>
    );
}

interface InterfaceItemProps {
    iface: string;
    isInUse: boolean;
    supportsTethering: boolean;
    isUsable?: boolean;
    operatingMode?: string;
    state?: WirelessTetheringState;
}

function InterfaceItem({ iface, isInUse, supportsTethering, isUsable = true, operatingMode, state = "Idle" }: InterfaceItemProps) {
    const [expanded, setExpanded] = useState(false);
    const [inUse, setInUse] = useState<boolean>(false);
    const theme = useTheme();

    useEffect(() => {
        setInUse(isInUse);
    }, [isInUse]);

    const secondaryText = !isUsable && operatingMode
        ? `In use: ${operatingMode}`
        : (inUse ? `Status: ${state}` : "");

    return (
        <Box sx={{
            borderBottom: inUse ? "none" : "1px solid rgba(255,255,255,0.05)",
            bgcolor: inUse ? 'rgba(76, 175, 80, 0.05)' : (expanded ? 'rgba(0,0,0,0.5)' : 'rgba(255,255,255,0.05)'),
            borderRadius: theme.shape.borderRadius,
            mt: 1, mb: 1
        }}>
            <ListItem
                onClick={() => setExpanded(!expanded)}
                sx={{ cursor: 'pointer', py: 1.5 }}
            >
                <ListItemText
                    primary={iface}
                    secondary={secondaryText}
                    slotProps={{
                        primary: { sx: { fontWeight: inUse ? 600 : 500 } }
                    }}
                />
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, textAlign: "center" }}>
                    <Box sx={{ display: "flex", flexDirection: "column" }}>
                        <Box sx={{
                            display: "flex",
                            borderColor: supportsTethering ? theme.palette.success.dark : theme.palette.error.dark,
                            borderStyle: "solid",
                            borderRadius: 5,
                            p: 1,
                            justifyContent: "center"
                        }}>
                            {supportsTethering ? <WifiTetheringIcon sx={{ fontSize: 24 }} /> : <WifiTetheringErrorIcon sx={{ fontSize: 24 }} />}
                        </Box>
                        <Typography variant="caption">AP mode</Typography>
                    </Box>
                    <Box sx={{ display: "flex", flexDirection: "column" }}>
                        <Box sx={{
                            display: "flex",
                            borderColor: supportsTethering ? theme.palette.success.dark : theme.palette.error.dark,
                            borderStyle: "solid",
                            borderRadius: 5,
                            p: 1,
                            justifyContent: "center",
                            alignItems: "center",
                            gap: 0.5
                        }}>
                            {supportsTethering ? <WifiTetheringIcon sx={{ fontSize: 20 }} /> : <WifiTetheringErrorIcon sx={{ fontSize: 20 }} />}
                            <AddIcon sx={{ fontSize: 16 }} />
                            {supportsTethering ? <WifiIcon sx={{ fontSize: 20 }} /> : <WifiOffIcon sx={{ fontSize: 20 }} />}
                        </Box>
                        <Typography variant="caption">Concurrent</Typography>
                    </Box>
                </Box>
            </ListItem>

            {expanded && (
                <Box sx={{ p: 2, display: 'flex', gap: 1, borderRadius: 1, mr: 1, ml: 1, mb: 1 }}>
                    {inUse ? (
                        <Button
                            variant="contained"
                            color="error"
                            size="small"
                            onClick={() => removeTetherDevice(iface)}
                        >
                            Remove
                        </Button>
                    ) : (
                        <Box>
                            <Typography variant="body2" sx={{ mb: 2 }}>
                                AP Mode: Declares if we found support to host a wifi network from this device.
                                <br /><br />
                                Concurrent mode: Declares if we found support to host a wifi network from this device as well as being able to connect to a separate wifi network simultaneously.
                            </Typography>
                            <Box sx={{ display: "flex", gap: 1 }}>
                                <Button
                                    variant="contained"
                                    size="small"
                                    onClick={() => useTetherDevice(iface)}
                                    sx={{ textTransform: 'none', px: 3 }}
                                >
                                    Use
                                </Button>
                            </Box>
                        </Box>
                    )}
                </Box>
            )}
        </Box>
    );
}