import { useEffect, useState } from "react";
import {
    Box,
    Button,
    CircularProgress,
    Divider,
    Stack,
    Typography,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import LinkOffIcon from "@mui/icons-material/LinkOff";
import LanIcon from "@mui/icons-material/Lan";
import { useTranslation } from "react-i18next";

import type {
    WifiInterfaceState,
    WirelessNetworkInterface,
} from "../../../types/types";
import { wifiApi } from "../../../api/requests/networking/wifi";
import { useSseSelector } from "../../../hooks/useSseSelector";
import WifiConnectDialog from "./../wifi/WifiNetworkConnectDialog";
import { WifiSignalIcon } from "../../../components/icons/WifiIcon";
import NetworkDetail from "../NetworkDetail";

interface WifiClientCardProps {
    iface: WirelessNetworkInterface;
}

export default function WifiClientCard({
    iface,
}: WifiClientCardProps) {
    const { t } = useTranslation();

    const wifiState = useSseSelector(
        state => state.wifiState?.[iface.interfaceName],
    );

    const [apiState, setApiState] =
        useState<WifiInterfaceState | null>(null);

    const [connectDialogOpen, setConnectDialogOpen] =
        useState(false);

    const [disconnecting, setDisconnecting] =
        useState(false);

    const [releasing, setReleasing] =
        useState(false);

    useEffect(() => {
        let cancelled = false;

        void wifiApi
            .getConnection(iface.interfaceName)
            .then(state => {
                if (!cancelled) {
                    setApiState(state);
                }
            })
            .catch(error => {
                console.error(
                    `Failed to get WiFi state for ${iface.interfaceName}:`,
                    error,
                );

                if (!cancelled) {
                    setApiState(null);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [iface.interfaceName]);

    const currentState = wifiState ?? apiState;

    const connected =
        currentState?.state === "Connected" &&
        currentState.network != null;

    const acquired =
        currentState?.state === "Acquired" &&
        currentState.operatingMode === "Client";

    const network = currentState?.network;

    const busy = disconnecting || releasing;

    const handleDisconnect = async () => {
        if (busy) {
            return;
        }

        setDisconnecting(true);

        try {
            await wifiApi.disconnect(iface.interfaceName);
        } catch (error) {
            console.error(
                `Failed to disconnect WiFi ${iface.interfaceName}:`,
                error,
            );
        } finally {
            setDisconnecting(false);
        }
    };

    const handleRelease = async () => {
        if (busy) {
            return;
        }

        setReleasing(true);

        try {
            await wifiApi.release(iface.interfaceName);
        } catch (error) {
            console.error(
                `Failed to release WiFi lease on ${iface.interfaceName}:`,
                error,
            );
        } finally {
            setReleasing(false);
        }
    };

    const ssid = network?.ssid ?? t("network.wifi.not_connected");
    const ipv4 = currentState?.ipv4 ?? "-";

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
            {/* Main connection */}
            <Box
                sx={{
                    p: 2,
                    display: "flex",
                    alignItems: "center",
                    gap: 1.5,
                    minHeight: 82,
                }}
            >
                {connected && network ? (
                    <WifiSignalIcon
                        isSecure={network.isSecure}
                        strength={network.signalPercent}
                        sx={{
                            fontSize: 46,
                            color: "success.main",
                            flexShrink: 0,
                        }}
                    />
                ) : (
                    <Box
                        sx={{
                            width: 46,
                            height: 46,
                            borderRadius: 2,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            backgroundColor: "action.hover",
                            color: "text.secondary",
                            flexShrink: 0,
                        }}
                    >
                        <SearchIcon />
                    </Box>
                )}

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
                        sx={{
                            mt: 0.5,
                            fontFamily: connected
                                ? "monospace"
                                : undefined,
                        }}
                    >
                        {connected
                            ? ipv4
                            : t("network.wifi.not_connected")}
                    </Typography>
                </Box>

                {connected && (
                    <Button
                        variant="outlined"
                        color="error"
                        startIcon={
                            disconnecting ? (
                                <CircularProgress
                                    size={18}
                                    color="inherit"
                                />
                            ) : (
                                <LinkOffIcon />
                            )
                        }
                        onClick={() => void handleDisconnect()}
                        disabled={busy}
                        sx={{
                            flexShrink: 0,
                            minWidth: 130,
                        }}
                    >
                        {t("network.disconnect")}
                    </Button>
                )}
            </Box>

            <Divider />

            {/* Connection details */}
            <Box sx={{ p: 1.5 }}>
                <Stack
                    sx={{
                        flexDirection: "row",
                        flexWrap: "wrap",
                        gap: 1,
                    }}
                >
                    <NetworkDetail
                        label="Signal"
                        value={
                            connected && network
                                ? `${network.signalPercent}%`
                                : "-"
                        }
                        emphasis={connected}
                    />

                    <NetworkDetail
                        label="Frequency"
                        value={
                            connected &&
                                network?.frequencyMhz
                                ? `${network.frequencyMhz} MHz`
                                : "-"
                        }
                    />

                    <NetworkDetail
                        label="Channel"
                        value={
                            connected && network
                                ? network.channel?.toString() ?? "-"
                                : "-"
                        }
                    />

                    <NetworkDetail
                        label="Bandwidth"
                        value={
                            connected &&
                                network?.bandwidthMhz
                                ? `${network.bandwidthMhz} MHz`
                                : "-"
                        }
                    />

                    <NetworkDetail
                        label="Security"
                        value={
                            connected && network
                                ? network.securityType
                                : "-"
                        }
                    />
                </Stack>

                <Stack
                    sx={{
                        flexDirection: "row",
                        alignItems: "center",
                        gap: 1,
                        mt: 1.25,
                        px: 0.5,
                    }}
                >
                    <LanIcon
                        sx={{
                            fontSize: 18,
                            color: "text.secondary",
                        }}
                    />

                    <Typography
                        variant="caption"
                        color="text.secondary"
                    >
                        BSSID
                    </Typography>

                    <Typography
                        variant="caption"
                        sx={{
                            fontFamily: "monospace",
                        }}
                    >
                        {connected && network
                            ? network.bssid
                            : "-"}
                    </Typography>
                </Stack>
            </Box>

            <Divider />

            {/* Actions */}
            <Box sx={{ p: 1 }}>
                <Stack
                    direction="row"
                    spacing={1}
                >
                    <Button
                        fullWidth
                        variant="contained"
                        startIcon={<SearchIcon />}
                        onClick={() => setConnectDialogOpen(true)}
                        disabled={busy}
                    >
                        {t("network.wifi.search")}
                    </Button>

                    {acquired && (
                        <Button
                            variant="outlined"
                            color="error"
                            startIcon={
                                releasing ? (
                                    <CircularProgress
                                        size={18}
                                        color="inherit"
                                    />
                                ) : (
                                    <LinkOffIcon />
                                )
                            }
                            onClick={() => void handleRelease()}
                            disabled={busy}
                            sx={{
                                flexShrink: 0,
                                minWidth: 110,
                            }}
                        >
                            {t("network.release")}
                        </Button>
                    )}
                </Stack>
            </Box>

            <WifiConnectDialog
                open={connectDialogOpen}
                ifName={iface.interfaceName}
                onClose={() => setConnectDialogOpen(false)}
            />
        </Box>
    );
}