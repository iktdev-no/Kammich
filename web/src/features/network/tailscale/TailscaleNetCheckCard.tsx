import { useEffect, useState } from "react";
import {
    Box,
    Button,
    CircularProgress,
    Typography,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import NetworkCheckIcon from "@mui/icons-material/NetworkCheck";
import type { TailscaleNetcheck } from "../../../types/types";
import { tailscaleApi } from "../../../api/requests/networking/tailscale";
import { useTranslation } from "react-i18next";

function NetcheckStatusPill({
    label,
    enabled,
}: {
    label: string;
    enabled: boolean;
}) {
    const { t } = useTranslation();

    return (
        <Box
            sx={{
                display: "inline-flex",
                alignItems: "stretch",
                borderRadius: 999,
                overflow: "hidden",
                backgroundColor: "action.hover",
            }}
        >
            <Box
                sx={{
                    px: 1,
                    py: 0.5,
                    backgroundColor: enabled
                        ? "success.main"
                        : "error.main",
                    display: "flex",
                    alignItems: "center",
                    borderRadius: 999,
                }}
            >
                <Typography
                    variant="caption"
                    sx={{
                        fontWeight: 600,
                        color: enabled
                            ? "success.contrastText"
                            : "error.contrastText",
                    }}
                >
                    {label}
                </Typography>
            </Box>

            <Box
                sx={{
                    px: 1,
                    py: 0.5,
                    display: "flex",
                    alignItems: "center",
                }}
            >
                <Typography
                    variant="caption"
                    sx={{
                        fontWeight: 600,
                        color: enabled
                            ? "success.main"
                            : "error.main",
                    }}
                >
                    {enabled ? t("common.yes") : t("common.no")}
                </Typography>
            </Box>
        </Box>
    );
}

function NetcheckValue({
    label,
    value,
}: {
    label: string;
    value: string;
}) {
    return (
        <Box
            sx={{
                display: "flex",
                alignItems: "center",
                gap: 1,
            }}
        >
            <Box
                sx={{
                    px: 1,
                    py: 0.5,
                    borderRadius: 999,
                    backgroundColor: "action.hover",
                }}
            >
                <Typography
                    variant="caption"
                    sx={{
                        color: "text.secondary",
                        fontWeight: 600,
                    }}
                >
                    {label}
                </Typography>
            </Box>

            <Typography
                variant="body2"
                sx={{
                    fontFamily: "monospace",
                    fontWeight: 500,
                }}
            >
                {value}
            </Typography>
        </Box>
    );
}

export default function TailscaleNetCheckCard() {
    const { t } = useTranslation();

    const [isLoading, setLoading] = useState(false);
    const [netcheck, setNetcheck] =
        useState<TailscaleNetcheck | null>(null);

    const runNetcheck = async () => {
        if (isLoading) {
            return;
        }

        setLoading(true);

        try {
            const result = await tailscaleApi.getNetcheck();
            setNetcheck(result);
        } catch (error) {
            console.error("Netcheck feilet:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void runNetcheck();
    }, []);

    return (
        <Box
            sx={{
                backgroundColor: "background.paper",
                borderRadius: 3,
                p: 1.5,
            }}
        >
            <Box
                sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                }}
            >
                <Box
                    sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                    }}
                >
                    <NetworkCheckIcon
                        sx={{ color: "text.secondary" }}
                    />

                    <Typography
                        variant="subtitle2"
                        sx={{ color: "text.secondary" }}
                    >
                        {t("network.tailscale.netcheck")}
                    </Typography>
                </Box>

                <Button
                    size="small"
                    startIcon={
                        isLoading
                            ? <CircularProgress size={14} />
                            : <RefreshIcon />
                    }
                    onClick={() => void runNetcheck()}
                    disabled={isLoading}
                >
                    {t("common.refresh")}
                </Button>
            </Box>

            {netcheck && (
                <Box
                    sx={{
                        display: "flex",
                        flexWrap: "wrap",
                        gap: 1,
                        mt: 1.5,
                    }}
                >
                    <NetcheckStatusPill
                        label="UDP"
                        enabled={netcheck.udp}
                    />

                    <NetcheckStatusPill
                        label="IPv4"
                        enabled={netcheck.ipv4}
                    />

                    <NetcheckStatusPill
                        label="IPv4 send"
                        enabled={netcheck.ipv4CanSend}
                    />

                    <NetcheckStatusPill
                        label="IPv6"
                        enabled={netcheck.ipv6}
                    />

                    <NetcheckStatusPill
                        label="IPv6 send"
                        enabled={netcheck.ipv6CanSend}
                    />

                    <NetcheckStatusPill
                        label="ICMPv4"
                        enabled={netcheck.icmpv4}
                    />

                    <NetcheckValue
                        label="DERP"
                        value={netcheck.preferredDerp?.toString() ?? "-"}
                    />

                    <NetcheckValue
                        label="IPv4"
                        value={netcheck.globalV4 ?? "-"}
                    />

                    <NetcheckValue
                        label="IPv6"
                        value={netcheck.globalV6 ?? "-"}
                    />

                    <NetcheckValue
                        label="Captive portal"
                        value={netcheck.captivePortal ?? "-"}
                    />
                </Box>
            )}
        </Box>
    );
}