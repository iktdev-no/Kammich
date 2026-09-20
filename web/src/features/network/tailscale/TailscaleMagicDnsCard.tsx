import { Box, Stack, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import type { TailscaleDns } from "../../../types/types";
import { tailscaleApi } from "../../../api/requests/networking/tailscale";

interface TailscaleMagicDnsCardProps { }

export default function TailscaleMagicDnsCard({ }: TailscaleMagicDnsCardProps) {
    const [tdns, setTdns] = useState<TailscaleDns | null>(null);
    const { t } = useTranslation();

    useEffect(() => {
        tailscaleApi.getDns()
            .then(dns => setTdns(dns))
            .catch(err => console.error("Klarte ikke å hente Tailscale DNS", err));
    }, []);

    if (!tdns) {
        return null;
    }

    return (
        <Stack
            sx={{
                flexDirection: "row",
                gap: 1,
                flexWrap: "wrap",
                justifyContent: "center"
            }}
        >
            <TailscaleStatusPill
                label="MagicDNS"
                enabled={tdns.magicDnsEnabled}
                yes={t("common.yes")}
                no={t("common.no")}
            />

            <TailscaleStatusPill
                label="Tailscale DNS"
                enabled={tdns.tailscaleDns}
                yes={t("common.yes")}
                no={t("common.no")}
            />
        </Stack>
    );
}

interface TailscaleStatusPillProps {
    label: string;
    enabled: boolean;
    yes: string;
    no: string;
}

function TailscaleStatusPill({
    label,
    enabled,
    yes,
    no,
}: TailscaleStatusPillProps) {
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
                    {enabled ? yes : no}
                </Typography>
            </Box>
        </Box>
    );
}