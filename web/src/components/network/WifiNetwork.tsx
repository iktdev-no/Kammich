import { useState } from "react";
import type { InterfaceActiveState, WifiNetwork } from "../../types/types";
import { Box, Button, CircularProgress, Collapse, Divider, TextField, Tooltip, Typography, useTheme } from "@mui/material";
import { WifiSignalIcon } from "../icons/WifiIcon";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { connectToWifi, disconnectFromWifi } from "../../api/requests/networking/connection";

export interface WifiNetworkProps {
    wifi: WifiNetwork
    overlappingSSIDFreq?: boolean
    state: InterfaceActiveState
    expanded: boolean
    onToggle: () => void
}

export default function WifiNetworkCard({ wifi, state, overlappingSSIDFreq, expanded, onToggle }: WifiNetworkProps) {
    const theme = useTheme();
    const freqRangeText = () => {
        if (wifi.frequencyMhz > 5000) {
            return "(5 GHz)"
        } else {
            return "(2.4 GHz)"
        }
    }

    if (!wifi) return null;

    return (
        <Box sx={{
            bgcolor: theme.palette.background.paper,
            borderRadius: 2.5,
            overflow: "hidden", // Viktig for at animasjonen ikke lekker utfor hjørnene
            borderWidth: 1,
            borderStyle: "solid",
            borderColor: (expanded ? theme.palette.primary.main : theme.palette.divider)
        }}>
            {/* Header-delen som kan klikkes */}
            <Box
                onClick={onToggle}
                sx={{
                    p: 2,
                    display: "flex",
                    alignItems: "center",
                    cursor: "pointer",
                    "&:hover": {
                        bgcolor: "rgba(255,255,255,0.02)"
                    }
                }}
            >
                <Typography sx={{ flexGrow: 1, fontWeight: 500 }}>
                    {wifi.ssid} {overlappingSSIDFreq && freqRangeText()}
                </Typography>

                <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>

                    <WifiStateTooltip state={state}>
                        <CircularProgress size={24} thickness={5} />
                    </WifiStateTooltip>

                    <WifiTooltip wifi={wifi}>
                        <WifiSignalIcon isSecure={wifi.isSecure} strength={wifi.signalPercent} />
                    </WifiTooltip>

                    {/* Valgfritt: Pil som roterer når kortet er åpent */}
                    <ExpandMoreIcon
                        sx={{
                            transform: expanded ? "rotate(180deg)" : "rotate(0deg)",
                            transition: "transform 0.2s ease-in-out",
                            color: "text.secondary"
                        }}
                    />
                </Box>
            </Box>

            {/* Aninert innhold via Collapse */}
            <Collapse in={expanded} timeout="auto" unmountOnExit>
                <WifiNetworkActionsCard
                    isConnected={state === "Connected"}
                    wifi={wifi}
                />
            </Collapse>
        </Box>
    );
}


interface WifiNetworkActionsCardProps {
    isConnected: boolean;
    wifi: WifiNetwork,
}

function WifiNetworkActionsCard({ isConnected, wifi }: WifiNetworkActionsCardProps) {
    const [password, setPassword] = useState<string | undefined>(undefined);

    const onConnect = (password?: string) => {
        connectToWifi(wifi.interfaceName, wifi.bssid, password)
    }

    const onDisconnect = () => {
        disconnectFromWifi(wifi.interfaceName)
    }

    return (
        <>
            <Divider />
            <Box sx={{ display: 'flex', gap: 1, borderRadius: 1, p: 2 }}>
                {isConnected ? (
                    <Button
                        variant="contained"
                        color="error"
                        size="small"
                        fullWidth
                        onClick={onDisconnect}
                    >
                        Disconnect
                    </Button>
                ) : (
                    <>
                        {wifi.isSecure && (
                            <TextField size="small" label="Password" type="password" fullWidth value={password} onChange={(e) => setPassword(e.target.value)} />
                        )}
                        <Button variant="contained" size="small"
                            onClick={() => onConnect(password)}
                            sx={{ textTransform: 'none', px: 3 }}>
                            Connect
                        </Button>
                    </>
                )}
            </Box>
        </>
    )
}

interface WifiStateTooltipProps {
    state: InterfaceActiveState;
    children?: React.ReactNode; // Tar inn ikonet (eller et annet element) som child
}

const WifiStateTooltip = ({ state, children }: WifiStateTooltipProps) => {
    const visibleStates: Array<InterfaceActiveState> = [
        "Connecting",
    ]

    if (!state || !visibleStates.includes(state)) {
        return null;
    }

    return (
        <Tooltip arrow
            title={
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, p: 0.5 }}>
                    <Typography variant="caption" sx={{ fontWeight: 600 }}>{state}</Typography>
                </Box>
            }>
            <Box sx={{ display: 'inline-flex', alignItems: 'center', cursor: 'default' }}>
                {children}
            </Box>
        </Tooltip>
    )
}


interface WifiTooltipProps {
    wifi: WifiNetwork;
    children?: React.ReactNode; // Tar inn ikonet (eller et annet element) som child
}

export const WifiTooltip = ({ wifi, children }: WifiTooltipProps) => {
    return (
        <Tooltip
            arrow
            title={
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, p: 0.5 }}>
                    <Typography variant="caption" sx={{ fontWeight: 600 }}>{wifi.ssid}</Typography>
                    <Typography variant="caption">BSSID: {wifi.bssid}</Typography>
                    <Typography variant="caption">Kanal: {wifi.channel ?? "Ukjent"} ({wifi.frequencyMhz} MHz)</Typography>
                    <Typography variant="caption">Sikkerhet: {wifi.securityType}</Typography>
                    <Typography variant="caption">Signal: {wifi.signalPercent}%</Typography>
                </Box>
            }
        >
            {/* Box sikrer at tooltipen hekter seg skikkelig på uavhengig av hva child-elementet er */}
            <Box sx={{ display: 'inline-flex', alignItems: 'center', cursor: 'default' }}>
                {children}
            </Box>
        </Tooltip>
    );
};