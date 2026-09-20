
import type { WifiNetwork } from "../../../types/types";
import { Box, Tooltip, Typography } from "@mui/material";

interface WifiTooltipProps {
    wifi: WifiNetwork;
    children?: React.ReactNode;
}

export const WifiTooltip = ({ wifi, children }: WifiTooltipProps) => {
    // Funksjon for å utlede band basert på frekvens i MHz
    const getBandLabel = (mhz?: number) => {
        if (!mhz) return null;
        if (mhz >= 2400 && mhz < 2500) return "2.4 GHz";
        if (mhz >= 5150 && mhz <= 5850) return "5 GHz";
        if (mhz > 5850) return "6 GHz";
        return `${mhz} MHz`;
    };

    const band = getBandLabel(wifi.frequencyMhz);

    return (
        <Tooltip
            arrow
            title={
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, p: 0.5 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1 }}>
                        <Typography variant="caption" sx={{ fontWeight: 600 }}>{wifi.ssid}</Typography>
                        {band && (
                            <Typography
                                variant="caption"
                                sx={{
                                    fontWeight: 700,
                                    fontSize: '0.65rem',
                                    bgcolor: 'rgba(255, 255, 255, 0.1)',
                                    px: 0.5,
                                    py: 0.2,
                                    borderRadius: 0.5
                                }}
                            >
                                {band}
                            </Typography>
                        )}
                    </Box>
                    <Typography variant="caption">BSSID: {wifi.bssid}</Typography>
                    <Typography variant="caption">Kanal: {wifi.channel ?? "Ukjent"} ({wifi.frequencyMhz} MHz)</Typography>
                    <Typography variant="caption">Sikkerhet: {wifi.securityType}</Typography>
                    <Typography variant="caption">Signal: {wifi.signalPercent}%</Typography>
                </Box>
            }
        >
            <Box sx={{ display: 'inline-flex', alignItems: 'center', cursor: 'default' }}>
                {children}
            </Box>
        </Tooltip>
    );
};