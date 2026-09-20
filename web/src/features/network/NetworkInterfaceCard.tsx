import {
    Box,
    Card,
    CardContent,
    Collapse,
    IconButton,
    Stack,
    Typography,
} from "@mui/material";
import {
    useState,
    type ReactNode,
} from "react";

import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import WifiIcon from "@mui/icons-material/Wifi";
import SettingsEthernetIcon from "@mui/icons-material/SettingsEthernet";

import type {
    NetworkInterface,
    NetworkInterfaceMode,
} from "../../types/types";

import { useWarningDialog } from "../../hooks/useWarningDialog";
import { useTranslation } from "react-i18next";

import { ethernetApi } from "../../api/requests/networking/ethernet";
import { wifiApi } from "../../api/requests/networking/wifi";
import { useSseSelector } from "../../hooks/useSseSelector";

interface NetworkCardProps {
    iface: NetworkInterface;
    onReset: () => void;
    children?: ReactNode;
}

export default function NetworkIterfaceCard({
    iface,
    onReset,
    children,
}: NetworkCardProps) {
    const { t } = useTranslation();
    const { showWarning, warningDialog } = useWarningDialog();

    const [isCollapsed, setIsCollapsed] =
        useState<boolean>(false);

    const [resetting, setResetting] =
        useState<boolean>(false);

    const [childKey, setChildKey] =
        useState<number>(0);

    const wifiState = useSseSelector(
        state => state.wifiState,
    );

    const ethernetState = useSseSelector(
        state => state.ethState,
    );

    const currentMode: NetworkInterfaceMode =
        iface.type === "Wifi"
            ? wifiState[iface.interfaceName]?.operatingMode ??
            iface.mode
            : ethernetState[iface.interfaceName]?.operatingMode ??
            iface.mode;

    const statusColor = {
        Client: "primary.main",
        Idle: "action.disabledBackground",
        External: "success.main",
        Tether: "warning.main",
    }[currentMode];

    const handleReset = async (): Promise<void> => {
        try {
            setResetting(true);

            if (iface.type === "Ethernet") {
                await ethernetApi.reset(
                    iface.interfaceName,
                );
            }

            if (iface.type === "Wifi") {
                await wifiApi.reset(
                    iface.interfaceName,
                );
            }

            setChildKey(prev => prev + 1);
            onReset();
        } catch (error: unknown) {
            console.error(
                "Klarte ikke å tilbakestille grensesnitt",
                error,
            );
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
                transition: "all 0.2s ease-in-out",
                "&:hover": {
                    borderColor:
                        "rgba(255,255,255,0.15)",
                    boxShadow:
                        "0 4px 20px rgba(0,0,0,0.2)",
                },
            }}
        >
            <CardContent>
                <Stack
                    direction="row"
                    sx={{
                        alignItems: "center",
                        cursor: "pointer",
                    }}
                    spacing={2}
                    onClick={() =>
                        setIsCollapsed(prev => !prev)
                    }
                >
                    <Box
                        sx={{
                            p: 1.5,
                            borderRadius: 2,
                            bgcolor: statusColor,
                            color:
                                currentMode === "Idle"
                                    ? "text.primary"
                                    : "primary.contrastText",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                        }}
                    >
                        {iface.type === "Ethernet" && (
                            <SettingsEthernetIcon />
                        )}

                        {iface.type === "Wifi" && (
                            <WifiIcon />
                        )}
                    </Box>

                    <Box sx={{ flexGrow: 1 }}>
                        <Typography
                            variant="h6"
                            sx={{ fontWeight: 600 }}
                        >
                            {iface.interfaceName}
                        </Typography>

                        <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{
                                fontFamily: "monospace",
                            }}
                        >
                            {iface.macAdress}
                        </Typography>
                    </Box>

                    <IconButton
                        disabled={resetting}
                        onClick={event => {
                            event.stopPropagation();

                            showWarning({
                                severity: "critical",
                                title: t(
                                    "network.reset.confirm.title",
                                ),
                                message: t(
                                    "network.reset.confirm.message",
                                    {
                                        interfaceName:
                                            iface.interfaceName,
                                    },
                                ),
                                onConfirm: () =>
                                    handleReset(),
                            });
                        }}
                        sx={{
                            "&:hover": {
                                color: "error.main",
                                backgroundColor:
                                    "rgba(211, 47, 47, 0.08)",
                            },
                        }}
                    >
                        <RestartAltIcon />
                    </IconButton>

                    <IconButton
                        onClick={event => {
                            event.stopPropagation();

                            setIsCollapsed(prev => !prev);
                        }}
                    >
                        <ExpandMoreIcon
                            sx={{
                                transform: isCollapsed
                                    ? "rotate(0deg)"
                                    : "rotate(180deg)",
                                transition:
                                    "transform 0.2s",
                            }}
                        />
                    </IconButton>
                </Stack>

                <Collapse
                    in={!isCollapsed}
                    unmountOnExit
                >
                    <Box sx={{ mt: 2, pt: 0 }}>
                        <Box key={childKey}>
                            {children}
                        </Box>
                    </Box>
                </Collapse>
            </CardContent>

            {warningDialog}
        </Card>
    );
}