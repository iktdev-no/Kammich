import { useEffect, useRef, useState } from "react";
import {
    Box,
    CircularProgress,
    Dialog,
    DialogContent,
    IconButton,
    List,
    Stack,
    Typography,
    useTheme,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import { useTranslation } from "react-i18next";

import type {
    WifiInterfaceStateType,
    WifiNetwork,
} from "../../../types/types";
import { useSseSelector } from "../../../hooks/useSseSelector";
import WifiNetworkCard from "./WifiNetwork";
import { VisibilityIcon } from "../../../components/icons/VisibilityIcon";

interface WifiConnectDialogProps {
    open: boolean;
    ifName: string;
    onClose: () => void;
}

export default function WifiConnectDialog({
    open,
    ifName,
    onClose,
}: WifiConnectDialogProps) {
    const theme = useTheme();
    const { t } = useTranslation();

    const [expandedBssid, setExpandedBssid] =
        useState<string | null>(null);

    const [showHidden, setShowHidden] =
        useState<boolean>(false);

    const [showScanning, setShowScanning] =
        useState<boolean>(false);

    const [connectingBssid, setConnectingBssid] =
        useState<string | null>(null);

    const scanStartedAt = useRef<number>(0);

    const scanStatus = useSseSelector(
        state => state.wifiScanStatuses?.[ifName],
    );

    const scanResult = useSseSelector(
        state => state.wifiScanResults?.[ifName],
    );

    const wifiState = useSseSelector(
        state => state.wifiState?.[ifName],
    );

    const isScanning = !!scanStatus?.isScanning;
    const networks = scanResult?.networks ?? [];

    useEffect(() => {
        if (!open || isScanning) {
            return;
        }

        const elapsed = Date.now() - scanStartedAt.current;
        const remaining = Math.max(0, 1000 - elapsed);

        const timer = window.setTimeout(() => {
            setShowScanning(false);
        }, remaining);

        return () => window.clearTimeout(timer);
    }, [isScanning, open]);

    useEffect(() => {
        if (
            connectingBssid == null ||
            wifiState?.state !== "Connected" ||
            wifiState.network?.bssid !== connectingBssid
        ) {
            return;
        }

        setConnectingBssid(null);
        onClose();
    }, [
        connectingBssid,
        wifiState?.state,
        wifiState?.network?.bssid,
        onClose,
    ]);

    const toggleNetwork = (bssid: string): void => {
        setExpandedBssid(current =>
            current === bssid ? null : bssid,
        );
    };

    const handleConnect = (bssid: string): void => {
        setConnectingBssid(bssid);
    };

    const getNetworkState = (
        network: WifiNetwork,
    ): WifiInterfaceStateType => {
        if (!wifiState) {
            return "Idle";
        }

        if (
            wifiState.state === "Connecting" &&
            wifiState.network?.bssid === network.bssid
        ) {
            return "Connecting";
        }

        if (
            wifiState.state === "Connected" &&
            wifiState.network?.bssid === network.bssid
        ) {
            return "Connected";
        }

        return "Idle";
    };

    const visibleNetworks = [...networks].sort((a, b) =>
        (a.ssid || "Hidden network").localeCompare(
            b.ssid || "Hidden network",
            undefined,
            { sensitivity: "base" },
        ),
    );

    const filteredNetworks = showHidden
        ? visibleNetworks
        : visibleNetworks.filter(network => !!network.ssid);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullScreen
        >
            <IconButton
                onClick={onClose}
                aria-label={t("common.cancel")}
                sx={{
                    position: "fixed",
                    top: 20,
                    right: 20,
                    color: "text.primary",
                    bgcolor: "action.hover",
                    "&:hover": {
                        bgcolor: "action.selected",
                    },
                    zIndex: 10,
                }}
            >
                <CloseIcon fontSize="large" />
            </IconButton>

            <DialogContent
                sx={{
                    p: { xs: 2, sm: 4 },
                    overflowY: "auto",
                    bgcolor: theme.palette.background.default,
                }}
            >
                <Stack
                    sx={{
                        width: "100%",
                        maxWidth: 900,
                        mx: "auto",
                        gap: 2,
                    }}
                >
                    <Typography
                        variant="h5"
                        sx={{
                            fontWeight: 600,
                            pr: 7,
                        }}
                    >
                        {t("network.wifi.connect")}
                    </Typography>

                    <Stack
                        direction="row"
                        sx={{
                            alignItems: "center",
                            justifyContent: "space-between",
                            minHeight: 40,
                        }}
                    >
                        <Box
                            sx={{
                                height: 40,
                                display: "flex",
                                alignItems: "center",
                            }}
                        >
                            {showScanning && (
                                <CircularProgress size={20} />
                            )}
                        </Box>

                        <VisibilityIcon
                            visible={showHidden}
                            onChange={setShowHidden}
                        />
                    </Stack>

                    <List
                        disablePadding
                        sx={{
                            display: "flex",
                            flexDirection: "column",
                            gap: 1,
                        }}
                    >
                        {filteredNetworks.map(network => {
                            const overlappingSSIDFreq =
                                networks.some(
                                    other =>
                                        other.ssid === network.ssid &&
                                        other.frequencyMhz !==
                                        network.frequencyMhz,
                                );

                            return (
                                <WifiNetworkCard
                                    key={`${network.bssid}-${network.ssid}`}
                                    wifi={network}
                                    state={getNetworkState(network)}
                                    expanded={
                                        expandedBssid === network.bssid
                                    }
                                    onToggle={() =>
                                        toggleNetwork(network.bssid)
                                    }
                                    onConnect={() =>
                                        handleConnect(network.bssid)
                                    }
                                    overlappingSSIDFreq={
                                        overlappingSSIDFreq
                                    }
                                />
                            );
                        })}
                    </List>

                    {!isScanning && filteredNetworks.length === 0 && (
                        <Box
                            sx={{
                                py: 4,
                                textAlign: "center",
                                color: "text.secondary",
                            }}
                        >
                            <Typography variant="body2">
                                {t("network.wifi_no_network")}
                            </Typography>
                        </Box>
                    )}

                    <Box
                        sx={{
                            height: "50vh",
                            minHeight: "50vh",
                            flexShrink: 0,
                        }}
                    />
                </Stack>
            </DialogContent>
        </Dialog>
    );
}