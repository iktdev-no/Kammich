import { Stack, Box, Typography } from "@mui/material";
import type { TailscaleStatus } from "../../../types/types";
import { useTranslation } from "react-i18next";

interface TailscaleStatusCardProps {
    state: TailscaleStatus;
}

export default function TailscaleStatusCard({
    state,
}: TailscaleStatusCardProps) {
    const { t } = useTranslation();
    return (
        <Stack sx={{ gap: 1 }}>
            <StatusItem
                label={t("network.tailscale.relay")}
                value={state.self.relay ?? "None"}
            />

            <StatusItem
                label={t("settings.info.version")}
                value={state.version?.split("-")[0] ?? t("common.none")}
            />

            <StatusItem
                label={t("network.tailscale.exit_node")}
                value={state.self.exitNode
                    ? t("common.yes")
                    : t("common.no")}
            />
        </Stack>
    );
}

interface StatusItemProps {
    label: string;
    value: string;
}

function StatusItem({
    label,
    value,
}: StatusItemProps) {
    return (
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
                variant="body1"
                sx={{
                    fontFamily: "monospace",
                    fontWeight: 500,
                }}
            >
                {value}
            </Typography>
        </Stack>
    );
}