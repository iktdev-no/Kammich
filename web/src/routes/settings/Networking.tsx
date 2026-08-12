import { useEffect, useState } from "react";
import { Box, Typography, Card, CardContent, Chip, Stack, Divider } from "@mui/material";
import EthernetIcon from '@mui/icons-material/SettingsEthernet';
import WifiIcon from '@mui/icons-material/Wifi';
import LanIcon from '@mui/icons-material/Lan'; // Litt finere for ethernet
import { getNetworkInterfaces } from "../../api/requests/networking/networking";
import type { NetworkInterface, EthernetNetworkInterface, WirelessNetworkInterface } from "../../types/types";

export default function Networking() {
    const [interfaces, setInterfaces] = useState<Array<NetworkInterface>>([]);

    useEffect(() => {
        getNetworkInterfaces().then(ifaces => setInterfaces(ifaces));
    }, []);

    return (
        <Box sx={{ p: 4, maxWidth: 800, mx: 'auto' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>Nettverksgrensesnitt</Typography>
            <Stack spacing={2}>
                {interfaces.map((iface, i) => (
                    <NetworkCard key={iface.interfaceName || i} iface={iface} />
                ))}
            </Stack>
        </Box>
    );
}

function NetworkCard({ iface }: { iface: NetworkInterface }) {
    const isWifi = iface.type === "Wifi";

    return (
        <Card sx={{ borderRadius: 3, boxShadow: 2, transition: '0.3s', '&:hover': { boxShadow: 6 } }}>
            <CardContent>
                <Stack direction="row" alignItems="center" spacing={2}>
                    <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: isWifi ? '#e3f2fd' : '#f5f5f5' }}>
                        {isWifi ? <WifiIcon color="primary" /> : <EthernetIcon color="action" />}
                    </Box>
                    <Box sx={{ flexGrow: 1 }}>
                        <Typography variant="h6">{iface.interfaceName}</Typography>
                        <Typography variant="caption" color="text.secondary">MAC: {iface.macAdress}</Typography>
                    </Box>
                    <Chip label={iface.mode} variant="outlined" size="small" />
                </Stack>

                {isWifi && (
                    <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid #eee' }}>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                            Kapasiteter:
                        </Typography>
                        <Stack direction="row" spacing={1} flexWrap="wrap">
                            {(iface as WirelessNetworkInterface).caps?.map(cap => (
                                <Chip key={cap} label={cap} size="small" color="primary" sx={{ mb: 0.5 }} />
                            ))}
                        </Stack>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}