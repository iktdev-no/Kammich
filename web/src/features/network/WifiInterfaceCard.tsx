import { Box, ButtonBase, Stack, Typography } from "@mui/material";
import WifiIcon from "@mui/icons-material/Wifi";
import WifiTetheringIcon from "@mui/icons-material/WifiTethering";
import { useTranslation } from "react-i18next";
import type { WirelessNetworkInterface } from "../../types/types";
import { wifiApi } from "../../api/requests/networking/wifi";
import { useSseSelector } from "../../hooks/useSseSelector";
import WifiClientCard from "./wifi/WifiClientCard";
import WifiTetherCard from "./wifi/WifiTetherCard";
import { useEffect } from "react";

interface WifiInterfaceCardProps {
    iface: WirelessNetworkInterface;
}

export default function WifiInterfaceCard({
    iface,
}: WifiInterfaceCardProps) {
    const { t } = useTranslation();

    useEffect(() => {
        void wifiApi.startNetworkScan(iface.interfaceName);

        return () => {
            void wifiApi.stopNetworkScan(iface.interfaceName);
        };
    }, [iface.interfaceName]);

    const wifiInterfaceStates =
        useSseSelector(state => state.wifiState) || {};

    const wifiState = wifiInterfaceStates[iface.interfaceName];

    const supportsClient = iface.caps.includes("STA");
    const supportsTether = iface.caps.includes("AP");

    const displayMode = wifiState?.operatingMode ?? iface.mode;

    const handleClient = () => {
        void wifiApi.useClientDevice(iface.interfaceName);
    };

    const handleTether = () => {
        void wifiApi.useTetherDevice(iface.interfaceName);
    };

    if (displayMode === "External") {
        return null;
    }

    if (displayMode === "Client") {
        return <WifiClientCard iface={iface} />;
    }

    if (displayMode === "Tether") {
        return <WifiTetherCard iface={iface} />;
    }

    const modeCardSx = {
        flex: 1,
        minWidth: 0,
        border: 1,
        borderColor: "divider",
        borderRadius: 3,
        bgcolor: "background.paper",
        p: 2,
        textAlign: "left",
        transition: "border-color 0.15s ease, background-color 0.15s ease",
        "&:hover": {
            borderColor: "text.secondary",
            bgcolor: "action.hover",
        },
        "&:active": {
            bgcolor: "action.selected",
        },
    };

    return (
        <Box sx={{ width: "100%" }}>
            <Typography
                variant="subtitle2"
                color="text.secondary"
                sx={{
                    mb: 1,
                    px: 0.5,
                }}
            >
                {t("network.operation_mode")}
            </Typography>

            <Stack
                direction="row"
                spacing={1.5}
                sx={{ width: "100%" }}
            >
                {supportsClient && (
                    <ButtonBase
                        sx={modeCardSx}
                        onClick={handleClient}
                    >
                        <Stack
                            direction="row"
                            spacing={1.5}
                            sx={{
                                alignItems: "center",
                                width: "100%",
                            }}
                        >
                            <Box
                                sx={{
                                    width: 44,
                                    height: 44,
                                    borderRadius: 2,
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "center",
                                    bgcolor: "primary.main",
                                    color: "primary.contrastText",
                                    flexShrink: 0,
                                }}
                            >
                                <WifiIcon />
                            </Box>

                            <Box>
                                <Typography
                                    variant="subtitle1"
                                    sx={{ fontWeight: 600 }}
                                >
                                    {t("network.wifi.connect")}
                                </Typography>

                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{ mt: 1 }}
                                >
                                    {t("network.wifi.connect_description")}
                                </Typography>
                            </Box>
                        </Stack>
                    </ButtonBase>
                )}

                {supportsTether && (
                    <ButtonBase
                        sx={modeCardSx}
                        onClick={handleTether}
                    >
                        <Stack
                            direction="row"
                            spacing={1.5}
                            sx={{
                                alignItems: "center",
                                width: "100%",
                            }}
                        >
                            <Box
                                sx={{
                                    width: 44,
                                    height: 44,
                                    borderRadius: 2,
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "center",
                                    bgcolor: "warning.main",
                                    color: "warning.contrastText",
                                    flexShrink: 0,
                                }}
                            >
                                <WifiTetheringIcon />
                            </Box>

                            <Box>
                                <Typography
                                    variant="subtitle1"
                                    sx={{ fontWeight: 600 }}
                                >
                                    {t("network.tether.title")}
                                </Typography>

                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{ mt: 1 }}
                                >
                                    {t("network.tether.description")}
                                </Typography>
                            </Box>
                        </Stack>
                    </ButtonBase>
                )}
            </Stack>
        </Box>
    );
}