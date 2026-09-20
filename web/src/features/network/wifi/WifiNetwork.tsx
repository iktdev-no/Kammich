import type { WifiInterfaceStateType, WifiNetwork } from "../../../types/types";
import { Box, CircularProgress, Collapse, Tooltip, Typography, useTheme } from "@mui/material";
import { WifiSignalIcon } from "../../../components/icons/WifiIcon";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { WifiTooltip } from "./WifiNetworkTooltip";
import WifiNetworkActionsCard from "./WifiNetworkActionCard";

export interface WifiNetworkProps {
    wifi: WifiNetwork
    overlappingSSIDFreq?: boolean
    state: WifiInterfaceStateType
    expanded: boolean
    onToggle: () => void,
    onConnect: () => void,
}

export default function WifiNetworkCard({ wifi, state, overlappingSSIDFreq, expanded, onToggle, onConnect }: WifiNetworkProps) {
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
            overflow: "hidden",
            borderWidth: 1,
            borderStyle: "solid",
            borderColor: (expanded ? theme.palette.primary.main : theme.palette.divider)
        }}>
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

                    <ExpandMoreIcon
                        sx={{
                            transform: expanded ? "rotate(180deg)" : "rotate(0deg)",
                            transition: "transform 0.2s ease-in-out",
                            color: "text.secondary"
                        }}
                    />
                </Box>
            </Box>

            <Collapse in={expanded} timeout="auto" unmountOnExit>
                <WifiNetworkActionsCard
                    isConnected={state === "Connected"}
                    wifi={wifi}
                    onConnect={onConnect}
                />
            </Collapse>
        </Box>
    );
}




interface WifiStateTooltipProps {
    state: WifiInterfaceStateType;
    children?: React.ReactNode;
}

const WifiStateTooltip = ({ state, children }: WifiStateTooltipProps) => {
    const visibleStates: Array<WifiInterfaceStateType> = [
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



