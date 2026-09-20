import { useEffect, useState } from "react";
import {
    Box,
    Button,
    CircularProgress,
    Divider,
    Stack,
    Typography,
} from "@mui/material";
import WifiTetheringIcon from "@mui/icons-material/WifiTethering";
import WifiTetheringOffIcon from "@mui/icons-material/WifiTetheringOff";
import StopCircleOutlinedIcon from "@mui/icons-material/StopCircleOutlined";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SettingsIcon from "@mui/icons-material/Settings";
import { useTranslation } from "react-i18next";

import type {
    WifiInterfaceState,
    WifiTetherAP,
    WirelessNetworkInterface,
} from "../../../types/types";
import { wifiApi } from "../../../api/requests/networking/wifi";
import { useSseSelector } from "../../../hooks/useSseSelector";
import NetworkDetail from "../NetworkDetail";
import AccessPointSettingsDialog from "../AccessPointSettingsDialog";

interface WifiTetherCardProps {
    iface: WirelessNetworkInterface;
}

export default function WifiTetherCard({
    iface,
}: WifiTetherCardProps) {
    const { t } = useTranslation();

    const [settingsOpen, setSettingsOpen] =
        useState<boolean>(false);

    const wifiState = useSseSelector(
        state => state.wifiState?.[iface.interfaceName],
    );

    const [apiState, setApiState] =
        useState<WifiInterfaceState | null>(null);

    const [ap, setAp] =
        useState<WifiTetherAP | null>(null);

    const [loading, setLoading] =
        useState(false);

    useEffect(() => {
        let cancelled = false;

        void Promise.all([
            wifiApi.getTether(iface.interfaceName),
            wifiApi.getAccessPoint(),
        ])
            .then(([state, accessPoint]) => {
                if (cancelled) {
                    return;
                }

                setApiState(state);
                setAp(accessPoint ?? null);
            })
            .catch(error => {
                console.error(
                    `Failed to get tether state for ${iface.interfaceName}:`,
                    error,
                );
            });

        return () => {
            cancelled = true;
        };
    }, [iface.interfaceName]);

    const currentState = wifiState ?? apiState;

    const state = currentState?.state ?? "Idle";

    const running = state === "Tethering";

    const starting =
        state === "Starting";

    const stopping =
        state === "Stopping";

    const busy =
        loading ||
        starting ||
        stopping;

    const ssid =
        currentState?.network?.ssid ??
        ap?.ssid ??
        "Kammich";

    const frequency =
        currentState?.network?.frequencyMhz;

    const channel =
        currentState?.network?.channel;

    const security =
        currentState?.network?.securityType ??
        ap?.security ??
        "-";

    const handleStart = async () => {
        if (busy) {
            return;
        }

        setLoading(true);

        try {
            await wifiApi.startTethering(
                iface.interfaceName,
            );
        } catch (error) {
            console.error(
                `Failed to start tethering on ${iface.interfaceName}:`,
                error,
            );
        } finally {
            setLoading(false);
        }
    };

    const handleStop = async () => {
        if (busy) {
            return;
        }

        setLoading(true);

        try {
            await wifiApi.stopTethering(
                iface.interfaceName,
            );
        } catch (error) {
            console.error(
                `Failed to stop tethering on ${iface.interfaceName}:`,
                error,
            );
        } finally {
            setLoading(false);
        }
    };

    const acquired =
        state === "Acquired" &&
        currentState?.operatingMode === "Tether";

    return (
        <Box
            sx={{
                width: "100%",
                borderRadius: 3,
                backgroundColor: "rgba(0, 0, 0, 0.22)",
                border: "1px solid",
                borderColor: "divider",
                overflow: "hidden",
            }}
        >
            {/* AP header */}
            <Box
                sx={{
                    p: 2,
                    display: "flex",
                    alignItems: "center",
                    gap: 1.5,
                }}
            >
                <Box
                    sx={{
                        width: 46,
                        height: 46,
                        borderRadius: 2,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        backgroundColor: running
                            ? "success.main"
                            : "action.hover",
                        color: running
                            ? "success.contrastText"
                            : "text.secondary",
                        flexShrink: 0,
                    }}
                >
                    {running ? (
                        <WifiTetheringIcon />
                    ) : (
                        <WifiTetheringOffIcon />
                    )}
                </Box>

                <Box
                    sx={{
                        flex: 1,
                        minWidth: 0,
                    }}
                >
                    <Typography
                        variant="h6"
                        sx={{
                            fontWeight: 600,
                            lineHeight: 1.2,
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                        }}
                    >
                        {ssid}
                    </Typography>

                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{ mt: 0.5 }}
                    >
                        {running
                            ? "Access point active"
                            : state}
                    </Typography>
                </Box>

                {running ? (
                    <Button
                        variant="contained"
                        color="error"
                        startIcon={
                            loading ? (
                                <CircularProgress
                                    size={18}
                                    color="inherit"
                                />
                            ) : (
                                <StopCircleOutlinedIcon />
                            )
                        }
                        onClick={() => void handleStop()}
                        disabled={busy}
                        sx={{
                            flexShrink: 0,
                            minWidth: 110,
                        }}
                    >
                        {t("network.stop")}
                    </Button>
                ) : acquired ? (
                    <Stack
                        direction="row"
                        spacing={1}
                    >
                        <Button
                            variant="contained"
                            color="success"
                            startIcon={
                                loading ? (
                                    <CircularProgress
                                        size={18}
                                        color="inherit"
                                    />
                                ) : (
                                    <PlayArrowIcon />
                                )
                            }
                            onClick={() => void handleStart()}
                            disabled={busy}
                            sx={{
                                minWidth: 100,
                            }}
                        >
                            {t("network.start")}
                        </Button>

                        <Button
                            variant="outlined"
                            color="error"
                            startIcon={<WifiTetheringOffIcon />}
                            onClick={() => void wifiApi.release(iface.interfaceName)}
                            disabled={busy}
                            sx={{
                                minWidth: 100,
                            }}
                        >
                            {t("network.release")}
                        </Button>
                    </Stack>
                ) : (
                    <Button
                        variant="contained"
                        color="success"
                        startIcon={
                            loading ? (
                                <CircularProgress
                                    size={18}
                                    color="inherit"
                                />
                            ) : (
                                <PlayArrowIcon />
                            )
                        }
                        onClick={() => void handleStart()}
                        disabled={busy}
                        sx={{
                            flexShrink: 0,
                            minWidth: 110,
                        }}
                    >
                        {t("network.start")}
                    </Button>
                )}
            </Box>

            <Divider />

            {/* AP details */}
            <Box sx={{ p: 1.5 }}>
                <Stack
                    sx={{
                        flexDirection: "row",
                        flexWrap: "wrap",
                        gap: 1,
                    }}
                >
                    <NetworkDetail
                        label="Security"
                        value={security}
                    />

                    <NetworkDetail
                        label="Frequency"
                        value={
                            frequency
                                ? `${frequency} MHz`
                                : "-"
                        }
                    />

                    <NetworkDetail
                        label="Channel"
                        value={
                            channel?.toString() ?? "-"
                        }
                    />

                    <NetworkDetail
                        label="Interface"
                        value={iface.interfaceName}
                    />
                </Stack>
            </Box>

            <Divider />

            {/* Configuration action */}
            <Box sx={{ p: 1 }}>
                <Button
                    fullWidth
                    variant="contained"
                    startIcon={<SettingsIcon />}
                    disabled={busy}
                    onClick={() => setSettingsOpen(true)}
                >
                    {t("network.tether.configure")}
                </Button>
            </Box>
            <AccessPointSettingsDialog
                open={settingsOpen}
                onClose={() => setSettingsOpen(false)}
            />
        </Box>
    );
}
