import { useEffect, useState } from "react";
import { Box, Typography, Card, CardContent, Chip, Stack, Button, CircularProgress } from "@mui/material";
import WifiIcon from '@mui/icons-material/Wifi';
import SettingsEthernetIcon from '@mui/icons-material/SettingsEthernet';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import { getNetworkInterfaces } from "../../api/requests/networking/networking";
import { resetNetworkInterface } from "../../api/requests/networking/networking"; // Antatt sti basert på koden din
import type { NetworkInterface, WirelessNetworkInterface } from "../../types/types";

export default function Networking() {
    const [interfaces, setInterfaces] = useState<Array<NetworkInterface>>([]);
    const [loading, setLoading] = useState<boolean>(true);

    const fetchInterfaces = () => {
        setLoading(true);
        getNetworkInterfaces()
            .then(ifaces => setInterfaces(ifaces))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        fetchInterfaces();
    }, []);

    return (
        <Box sx={{ p: { xs: 2, md: 4 }, maxWidth: 800, mx: 'auto' }}>
            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
                <Typography variant="h4" sx={{ fontWeight: 700, fontSize: { xs: "1.5rem", md: "2.125rem" } }}>
                    Nettverksgrensesnitt
                </Typography>
            </Box>

            {loading && interfaces.length === 0 ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
                    <CircularProgress />
                </Box>
            ) : (
                <Stack spacing={2}>
                    {interfaces.map((iface, i) => (
                        <NetworkCard key={iface.interfaceName || i} iface={iface} onReset={fetchInterfaces} />
                    ))}
                </Stack>
            )}
        </Box>
    );
}

function NetworkCard({ iface, onReset }: { iface: NetworkInterface; onReset: () => void }) {
    const [resetting, setResetting] = useState(false);
    const isWifi = iface.type === "Wifi";

    const handleReset = async () => {
        try {
            setResetting(true);
            await resetNetworkInterface(iface.interfaceName);
            onReset(); // Hent oppdaterte data etter reset
        } catch (err) {
            console.error("Klarte ikke å tilbakestille grensesnitt", err);
        } finally {
            setResetting(false);
        }
    };

    return (
        <Card
            sx={{
                backgroundColor: "background.paper",
                borderRadius: 3,
                border: "1px solid rgba(255,255,255,0.06)",
                transition: 'all 0.2s ease-in-out',
                '&:hover': {
                    borderColor: "rgba(255,255,255,0.15)",
                    boxShadow: "0 4px 20px rgba(0,0,0,0.2)"
                }
            }}
        >
            <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                <Stack direction={{ xs: "column", sm: "row" }} sx={{ alignItems: { xs: "flex-start", sm: "center" }, justifyContent: "space-between" }} spacing={2}>

                    {/* Venstre side: Ikon og navn */}
                    <Stack direction="row" spacing={2} sx={{ alignItems: "center", width: { xs: "100%", sm: "auto" } }}>
                        <Box
                            sx={{
                                p: 1.5,
                                borderRadius: 2,
                                bgcolor: isWifi ? 'primary.main' : 'action.selected',
                                color: isWifi ? 'primary.contrastText' : 'text.primary',
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center"
                            }}
                        >
                            {isWifi ? <WifiIcon /> : <SettingsEthernetIcon />}
                        </Box>
                        <Box sx={{ flexGrow: 1 }}>
                            <Typography variant="h6" sx={{ fontWeight: 600 }}>
                                {iface.interfaceName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
                                MAC: {iface.macAdress}
                            </Typography>
                        </Box>
                    </Stack>

                    {/* Høyre side: Mode-chip og Reset-knapp */}
                    <Stack direction="row" spacing={1.5} sx={{ alignItems: "center", width: { xs: "100%", sm: "auto" }, justifyContent: { xs: "space-between", sm: "flex-end" } }}>
                        <Chip
                            label={iface.mode}
                            variant="outlined"
                            size="small"
                            sx={{ fontWeight: 500, textTransform: "capitalize" }}
                        />
                        <Button
                            variant="outlined"
                            color="error"
                            size="small"
                            startIcon={<RestartAltIcon />}
                            onClick={handleReset}
                            disabled={resetting}
                            sx={{
                                borderColor: "rgba(255,75,75,0.3)",
                                '&:hover': { borderColor: "error.main", bgcolor: "error.dark" }
                            }}
                        >
                            {resetting ? "Tilbakestiller..." : "Nullstill"}
                        </Button>
                    </Stack>
                </Stack>

                {/* Wi-Fi Spesifikke kapasiteter */}
                {isWifi && (iface as WirelessNetworkInterface).caps && (
                    <Box sx={{ mt: 2.5, pt: 2, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1, fontWeight: 500 }}>
                            Kapasiteter:
                        </Typography>
                        <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap", gap: 0.5 }}>
                            {(iface as WirelessNetworkInterface).caps.map(cap => (
                                <Chip
                                    key={cap}
                                    label={cap}
                                    size="small"
                                    sx={{
                                        bgcolor: "rgba(255,255,255,0.04)",
                                        color: "text.secondary",
                                        border: "1px solid rgba(255,255,255,0.06)"
                                    }}
                                />
                            ))}
                        </Stack>
                    </Box>
                )}
            </CardContent>
        </Card>
    );
}