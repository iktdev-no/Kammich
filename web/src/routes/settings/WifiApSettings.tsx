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
import { useSseSelector } from "../../sse/useSseSelector";

import type { WifiNetworkTether, WifiSecurityType, WifiTetherAP, WirelessInterface } from "../../types/types";
import { WifiTetheringOff } from "@mui/icons-material";
import { addTetherDevice, getAp, getInterfaces, removeTetherDevice, setAp, startTethering, stopTethering } from "../../api/requests/networking/tethering";

export default function WifiApSettings() {
    const theme = useTheme();
    const useTether = useSseSelector(state => state.wifiTether);

    const [interfaces, setInterfaces] = useState<WirelessInterface[]>([]);
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
        getInterfaces().then((interfaces) => {
            setInterfaces(interfaces)
        });

        getApConfig();
    }, [useTether]);


    return (
        <Box sx={{ maxWidth: "1000px", mx: "auto", p: 4 }}>
            <Typography variant="h5" sx={{ fontWeight: 600, mb: 4, display: "flex", alignItems: "center", gap: 1 }}>
                <WifiTetheringIcon /> Access Point Settings
            </Typography>

            <Box>
                <Paper sx={{ p: 3, backgroundColor: theme.palette.background.paper, borderRadius: theme.shape.borderRadius, height: '100%' }}>
                    <Typography variant="h6" sx={{ mb: 2 }}>System Status</Typography>
                    {useTether?.state ? (
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                            <Typography>Interface: {useTether?.name}</Typography>
                            <Box sx={{ display: "flex", alignItems: "center", flexDirection: "row", gap: 1 }}>
                                <Typography>State:</Typography>
                                <Chip label={useTether?.state} size="small" color={useTether?.state === "Broadcasting" ? 'success' : 'warning'} />
                            </Box>
                            <Divider sx={{ my: 1 }} />
                            {useTether.network && (
                                <>
                                    <Typography variant="body2">SSID: {useTether.network.ssid}</Typography>
                                    <Typography variant="body2">Freq: {useTether.network.frequencyMhz} MHz</Typography>
                                </>
                            )}
                            <Box>
                                {useTether.state == "Idle" ? (
                                    <Button
                                        variant="contained"
                                        size="small"
                                        onClick={() => {
                                            startTethering(useTether.name)
                                        }}
                                    >
                                        Start
                                    </Button>
                                ) : (
                                    <Button
                                        variant="contained"
                                        color="error"
                                        size="small"
                                        onClick={() => {
                                            stopTethering(useTether.name)
                                        }}
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
                            <Typography>No interface configured</Typography>
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
                        <ActiveTetherDevice tether={useTether} />
                        <AvailableInterfaces activeTether={useTether} availableInterfaces={interfaces} />
                    </Box>
                    {/* VENSTRE: Kontrollpanel */}

                    {/* HØYRE: Status / Informasjon */}

                </Box>
            </Box>
        </Box >

    );
}

export function ActiveTetherDevice({ tether }: { tether: WifiNetworkTether | undefined }) {
    const [t, setT] = useState<WifiNetworkTether | undefined>()
    useEffect(() => {
        setT(tether)
    }, [tether])

    return (
        <Box>
            {t && (
                <InterfaceItem
                    iface={t.name}
                    supportsTethering={true}
                    isInUse={true}
                />
            )}
        </Box>
    )
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
            <Typography sx={{
                mt: 2,
                ml: 2,
                mb: 2,
            }} variant="h6">Wifi Network settings</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2 }}>
                <TextField
                    size="small"
                    label="SSID"
                    value={ssid}
                    onChange={(e) => setSsid(e.target.value)}
                    slotProps={{
                        inputLabel: { shrink: !!ssid }
                    }}
                />

                <TextField
                    size="small"
                    label="Password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
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
                        inputLabel: { shrink: !!ssid }
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
                        password: password,
                        security: security,
                        ssid: ssid
                    } as WifiTetherAP)}
                    disabled={!ssid} // Enkel validering
                >
                    Save Configuration
                </Button>
            </Box>
        </Box>
    );
}

function AvailableInterfaces({ availableInterfaces, activeTether }: { activeTether: WifiNetworkTether | undefined, availableInterfaces: Array<WirelessInterface> }) {
    const [interfaces, setInterfaces] = useState<Array<WirelessInterface>>([]);
    const [at, setActiveTether] = useState<WifiNetworkTether | undefined>()
    useEffect(() => {
        setInterfaces(availableInterfaces)
        setActiveTether(activeTether)
    }, [availableInterfaces, activeTether])


    return (
        <>
            {interfaces.length > 0 ? (
                <List disablePadding>
                    {interfaces.map((iface: WirelessInterface, i: number) => (
                        <InterfaceItem key={i} iface={iface.name} supportsTethering={iface.isAvailable} isInUse={false} />
                    ))}
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
                    <Typography>{(at ? `${at.name} was the only one available` : "No interface available")}</Typography>
                </Box>
            )}

        </>
    )
}

function InterfaceItem({ iface, isInUse,
    supportsTethering }: { iface: string, supportsTethering: boolean, isInUse: boolean }) {
    const [expanded, setExpanded] = useState(false);
    const [inUse, setInUse] = useState<boolean>(false)
    const theme = useTheme()


    useEffect(() => {
        setInUse(isInUse)
    }, [isInUse])

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
                    secondary={inUse ? "Is in use" : ""}
                    slotProps={{
                        primary: { sx: { fontWeight: inUse ? 600 : 500 } }
                    }}
                />
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, textAlign: "center" }}>
                    <Box sx={{
                        display: "flex",
                        flexDirection: "column"
                    }}>
                        <Box sx={{
                            display: "flex",
                            borderColor: supportsTethering ? theme.palette.success.dark : theme.palette.error.dark,
                            borderStyle: "solid",
                            borderRadius: 5,
                            p: 1,
                            justifyContent: "center"
                        }} >
                            {supportsTethering ? <WifiTetheringIcon sx={{ fontSize: 24, }} /> : <WifiTetheringErrorIcon sx={{ fontSize: 24 }} />}
                        </Box>
                        <Typography>AP mode</Typography>
                    </Box>
                    <Box sx={{
                        display: "flex",
                        flexDirection: "column"
                    }}>
                        <Box sx={{
                            display: "flex",
                            borderColor: supportsTethering ? theme.palette.success.dark : theme.palette.error.dark,
                            borderStyle: "solid",
                            borderRadius: 5,
                            p: 1,
                            justifyContent: "center"
                        }} >
                            {supportsTethering ? <WifiTetheringIcon sx={{ fontSize: 24 }} /> : <WifiTetheringErrorIcon sx={{ fontSize: 24 }} />}
                            <AddIcon sx={{ fontSize: 24 }} />
                            {supportsTethering ? <WifiIcon sx={{ fontSize: 24 }} /> : <WifiOffIcon sx={{ fontSize: 24 }} />}
                        </Box>
                        <Typography>Concurrent mode</Typography>
                    </Box>
                </Box>
            </ListItem>

            {/* Expanderbar seksjon */}
            {expanded && (
                <Box sx={{ p: 2, display: 'flex', gap: 1, borderRadius: 1, mr: 1, ml: 1, mb: 1 }}>
                    {inUse ? (
                        <Button
                            variant="contained"
                            color="error"
                            size="small"
                            onClick={() => {
                                removeTetherDevice(iface)
                            }}
                        >
                            Remove
                        </Button>
                    ) : (
                        <Box>
                            <Typography>
                                AP Mode: Declares if we found support to host a wifi network from this device
                                <br />
                                <br />
                                Concurrent mode: Declares if we found support to host a wifi network fron this device as well as being able to connect to a seperate wifi network siumltaniously
                            </Typography>
                            <Box sx={{ display: "flex", gap: 1 }}>
                                <Button variant="contained" size="small" onClick={() => {
                                    addTetherDevice(iface)
                                }} sx={{ textTransform: 'none', px: 3 }}>Use</Button>
                            </Box>
                        </Box>
                    )}
                </Box>
            )}
        </Box>
    )
}
