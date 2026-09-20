import { useEffect, useState } from "react";
import type { ChangeEvent } from "react";

import type {
    EthernetInterfaceState,
    NetworkInterface,
} from "../../types/types";

import { ethernetApi } from "../../api/requests/networking/ethernet";

import SupportIcon from "@mui/icons-material/Support";
import LinkOffIcon from "@mui/icons-material/LinkOff";
import LinkIcon from "@mui/icons-material/Link";
import WarningOutlined from "@mui/icons-material/WarningOutlined";

import CableStatusIcon from "../../components/icons/CableStatusIcon";

import {
    Stack,
    Box,
    Typography,
    Switch,
    Button,
    useTheme,
} from "@mui/material";

import { useCountdown } from "../../hooks/useCountdown";
import { useSseSelector } from "../../hooks/useSseSelector";

interface EthernetInterfaceCardProps {
    iface: NetworkInterface;
}

export default function EthernetInterfaceCard({
    iface,
}: EthernetInterfaceCardProps) {
    const [loading, setLoading] = useState<boolean>(true);
    const [ethIface, setEthIface] =
        useState<EthernetInterfaceState | null>(null);

    const theme = useTheme();

    const ethernetState = useSseSelector(
        state => state.ethState?.[iface.interfaceName],
    );

    const currentState = ethernetState ?? ethIface;

    const fetchMe = async (): Promise<void> => {
        setLoading(true);

        try {
            const conn = await ethernetApi.get(
                iface.interfaceName,
            );

            setEthIface(conn);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void fetchMe();
    }, [iface.interfaceName]);

    const runAction = async (
        action: () => Promise<unknown>,
    ): Promise<void> => {
        if (loading) {
            return;
        }

        setLoading(true);

        try {
            await action();
            await fetchMe();
        } catch (error: unknown) {
            console.error(
                `Ethernet action failed for ${iface.interfaceName}:`,
                error,
            );
        } finally {
            setLoading(false);
        }
    };

    const handleClient = (): void => {
        void runAction(() =>
            ethernetApi.startClient(
                iface.interfaceName,
            ),
        );
    };

    const handleEmergency = (): void => {
        void runAction(() =>
            ethernetApi.startTether(
                iface.interfaceName,
            ),
        );
    };

    const handleDisconnect = (): void => {
        void runAction(() =>
            ethernetApi.disconnect(
                iface.interfaceName,
            ),
        );
    };

    const handleEmergencyChanged = (
        event: ChangeEvent<HTMLInputElement>,
    ): void => {
        const enabled = event.target.checked;

        void runAction(() =>
            ethernetApi.setEmergency(
                iface.interfaceName,
                enabled,
            ),
        );
    };

    const clientActive =
        currentState?.state === "Connected" ||
        currentState?.state === "Connecting";

    const emergencyActive =
        currentState?.state === "Emergency";

    const disconnectedActive =
        currentState?.state === "Disconnected";

    const emergencyCountdown = useCountdown(
        currentState?.state === "Connecting"
            ? currentState.emergencyAt ?? null
            : null,
    );

    return (
        <Stack
            direction="column"
            spacing={1}
        >
            {emergencyCountdown !== null && (
                <Box
                    sx={{
                        backgroundColor:
                            theme.palette.warning.main,
                        color:
                            theme.palette.warning.contrastText,
                        borderRadius: 3,
                        p: 1.5,
                    }}
                >
                    <Stack
                        sx={{
                            flexDirection: "row",
                            alignItems: "center",
                            gap: 1,
                        }}
                    >
                        <WarningOutlined />

                        <Box sx={{ flexGrow: 1 }}>
                            <Typography variant="body2">
                                Emergency fallback
                            </Typography>

                            <Typography variant="caption">
                                No IPv4 address has been received.
                            </Typography>
                        </Box>

                        <Typography
                            variant="h4"
                            sx={{
                                fontWeight: 600,
                                lineHeight: 1,
                                fontVariantNumeric:
                                    "tabular-nums",
                            }}
                        >
                            {emergencyCountdown}s
                        </Typography>
                    </Stack>
                </Box>
            )}

            {/* Emergency fallback */}
            {emergencyCountdown === null && (
                <Box
                    sx={{
                        backgroundColor:
                            theme.palette.background.paper,
                        borderRadius: 3,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        p: 1,
                    }}
                >
                    <Stack>
                        <Typography>
                            Emergency fallback
                        </Typography>

                        <Typography variant="caption">
                            Interface will enter fallback if it
                            has not received DHCP within 90 sec
                        </Typography>
                    </Stack>

                    <Switch
                        checked={
                            currentState?.emergencyAllowed ??
                            false
                        }
                        disabled={loading}
                        onChange={handleEmergencyChanged}
                    />
                </Box>
            )}

            {/* Connection */}
            <Box
                sx={{
                    backgroundColor:
                        theme.palette.background.paper,
                    borderRadius: 3,
                    p: 1,
                }}
            >
                <Typography
                    variant="subtitle2"
                    sx={{
                        px: 1,
                    }}
                >
                    Connection
                </Typography>

                <Stack
                    direction={{
                        xs: "column",
                        sm: "row",
                    }}
                    spacing={1}
                    sx={{
                        p: 1,
                    }}
                >
                    <Button
                        color="success"
                        variant={
                            clientActive
                                ? "contained"
                                : "outlined"
                        }
                        startIcon={<LinkIcon />}
                        disabled={loading}
                        onClick={handleClient}
                        fullWidth
                        sx={{
                            minHeight: 48,
                            flex: {
                                sm: 1,
                            },
                        }}
                    >
                        Client
                    </Button>

                    <Button
                        color="warning"
                        variant={
                            emergencyActive
                                ? "contained"
                                : "outlined"
                        }
                        startIcon={<SupportIcon />}
                        disabled={loading}
                        onClick={handleEmergency}
                        fullWidth
                        sx={{
                            minHeight: 48,
                            flex: {
                                sm: 1,
                            },
                        }}
                    >
                        Emergency
                    </Button>

                    <Button
                        color="error"
                        variant={
                            disconnectedActive
                                ? "contained"
                                : "outlined"
                        }
                        startIcon={<LinkOffIcon />}
                        disabled={
                            loading ||
                            disconnectedActive
                        }
                        onClick={handleDisconnect}
                        fullWidth
                        sx={{
                            minHeight: 48,
                            flex: {
                                sm: 1,
                            },
                        }}
                    >
                        Disconnect
                    </Button>
                </Stack>
            </Box>

            {/* Info */}
            <Box
                sx={{
                    backgroundColor:
                        theme.palette.background.paper,
                    borderRadius: 3,
                    p: 1.5,
                }}
            >
                <Typography
                    variant="subtitle2"
                    sx={{
                        color: "text.secondary",
                    }}
                >
                    Info
                </Typography>

                <Stack
                    sx={{
                        flexDirection: "row",
                        alignItems: "center",
                        gap: 2,
                        mt: 1,
                        px: 1,
                    }}
                >
                    <Stack
                        sx={{
                            flexDirection: "row",
                            alignItems: "center",
                            gap: 1,
                        }}
                    >
                        <Box
                            sx={{
                                px: 1,
                                py: 0.25,
                                borderRadius: 999,
                                backgroundColor:
                                    theme.palette.action.hover,
                            }}
                        >
                            <Typography
                                variant="caption"
                                sx={{
                                    color: "text.secondary",
                                    fontWeight: 600,
                                }}
                            >
                                IPv4
                            </Typography>
                        </Box>

                        <Typography
                            variant="body1"
                            sx={{
                                fontFamily: "monospace",
                                fontWeight: 500,
                            }}
                        >
                            {currentState?.ipv4 ?? "None"}
                        </Typography>
                    </Stack>

                    <Stack
                        sx={{
                            flexDirection: "row",
                            alignItems: "center",
                            gap: 0.75,
                        }}
                    >
                        <CableStatusIcon
                            connected={
                                currentState?.carrier
                            }
                        />

                        <Typography variant="body1">
                            {currentState?.carrier
                                ? "Connected"
                                : "Disconnected"}
                        </Typography>
                    </Stack>
                </Stack>
            </Box>
        </Stack>
    );
}