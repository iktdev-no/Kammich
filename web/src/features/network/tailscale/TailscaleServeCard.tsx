import { useEffect, useState } from "react";
import { Box, CircularProgress, Typography, useTheme } from "@mui/material";
import LanguageIcon from "@mui/icons-material/Language";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import type { TailscaleServe } from "../../../types/types";
import { tailscaleApi } from "../../../api/requests/networking/tailscale";
import { useTranslation } from "react-i18next";

export default function TailscaleServeCard() {
    const { t } = useTranslation();
    const theme = useTheme();
    const [isLoading, setLoading] = useState<boolean>(false);
    const [serve, setServe] = useState<TailscaleServe[]>([]);

    useEffect(() => {
        setLoading(true);

        tailscaleApi.getServe()
            .then(tserve => setServe(tserve))
            .finally(() => setLoading(false));
    }, []);

    if (isLoading) {
        return (
            <Box
                sx={{
                    display: "flex",
                    justifyContent: "center",
                    py: 2,
                }}
            >
                <CircularProgress size={22} />
            </Box>
        );
    }

    if (serve.length === 0) {
        return (
            <Typography
                variant="body2"
                sx={{
                    color: "text.secondary",
                    py: 1,
                }}
            >
                {t("common.none")}
            </Typography>
        );
    }

    return (
        <Box sx={{
            backgroundColor: theme.palette.background.paper,
            borderRadius: 3,
            pl: 1,
            pr: 1
        }}>
            {serve.map((entry, index) => (
                <Box
                    key={`${entry.hostname}-${entry.path}-${index}`}
                    sx={{
                        py: 1.5,
                        borderBottom:
                            index < serve.length - 1
                                ? "1px solid"
                                : "none",
                        borderColor: "divider",
                    }}
                >
                    <Box
                        sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1,
                        }}
                    >
                        <LanguageIcon
                            fontSize="small"
                            sx={{ color: "text.secondary" }}
                        />

                        <Typography
                            variant="body1"
                            sx={{
                                fontWeight: 600,
                                fontFamily: "monospace",
                            }}
                        >
                            {entry.hostname}
                        </Typography>
                    </Box>

                    <Box
                        sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1,
                            mt: 0.75,
                            ml: 3.5,
                        }}
                    >
                        <Typography
                            variant="body2"
                            sx={{
                                fontFamily: "monospace",
                                color: "text.secondary",
                            }}
                        >
                            {entry.path || "/"}
                        </Typography>

                        <ArrowForwardIcon
                            sx={{
                                fontSize: 16,
                                color: "text.disabled",
                            }}
                        />

                        <Typography
                            variant="body2"
                            sx={{
                                fontFamily: "monospace",
                            }}
                        >
                            {entry.proxy}
                        </Typography>
                    </Box>
                </Box>
            ))}
        </Box>
    );
}