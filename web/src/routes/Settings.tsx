import { Box, Button, Chip, CircularProgress, LinearProgress, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { getKammichBackendVersion, requestKammichBackendUpdate } from "../api/requests/system";
import { useSseSelector } from "../sse/useSseSelector";
import type { AppUpdateProgress, Version } from "../types/types";
import MadeInNorwayBadge from "../components/icons/MadeInNorway";

export default function Settings() {
    const [version, setVersion] = useState<Version | null>(null);
    const [updateRequested, setUpdateRequested] = useState(false);

    const appUpdate: AppUpdateProgress =
        useSseSelector((state) => state.appUpdate);

    useEffect(() => {
        getKammichBackendVersion()
            .then(setVersion);
    }, []);

    useEffect(() => {
        if (
            appUpdate.status === "None" &&
            updateRequested
        ) {
            getKammichBackendVersion()
                .then(setVersion)
                .finally(() => setUpdateRequested(false));
        }
    }, [appUpdate.status, updateRequested]);

    const isUpdating =
        appUpdate.status !== "None" &&
        appUpdate.status !== "UpdateAvailable" &&
        appUpdate.status !== "Failed";

    const canUpdate =
        version?.updateAvailable &&
        version?.updatable &&
        !isUpdating &&
        !updateRequested;

    const requestUpdate = () => {
        setUpdateRequested(true);
        requestKammichBackendUpdate();
    };

    return (
        <Box
            sx={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                height: "100%",
            }}
        >
            <Box
                sx={{
                    pt: 5,
                    width: "100%",
                    maxWidth: 400,
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    gap: 1,
                }}
            >
                <Typography variant="body1">
                    Version
                </Typography>

                <Typography variant="body1">
                    {version?.kammichVersion ?? "..."}
                </Typography>

                {version?.updateAvailable && (
                    <Chip
                        label={`Ny versjon tilgjengelig: ${version.kammichGithubVersion}`}
                        color="primary"
                        size="small"
                        sx={{ mt: 1 }}
                    />
                )}

                {!version?.updatable && (
                    <Typography
                        variant="caption"
                        color="text.secondary"
                    >
                        Automatisk oppdatering er ikke tilgjengelig
                    </Typography>
                )}

                {appUpdate.status === "Downloading" && (
                    <Box
                        sx={{
                            width: "100%",
                            mt: 2
                        }}
                    >
                        <Typography
                            variant="body2"
                            sx={{ mb: 1 }}
                        >
                            {appUpdate.message ??
                                "Laster ned oppdatering..."}
                        </Typography>

                        <LinearProgress
                            variant={
                                appUpdate.progress != null
                                    ? "determinate"
                                    : "indeterminate"
                            }
                            value={appUpdate.progress ?? undefined}
                        />

                        {appUpdate.progress != null && (
                            <Typography
                                variant="caption"
                                color="text.secondary"
                            >
                                {appUpdate.progress} %
                            </Typography>
                        )}
                    </Box>
                )}

                {isUpdating &&
                    appUpdate.status !== "Downloading" && (
                        <Box
                            sx={{
                                display: "flex",
                                alignItems: "center",
                                gap: 1,
                                mt: 2
                            }}
                        >
                            <CircularProgress size={18} />

                            <Typography variant="body2">
                                {appUpdate.message ??
                                    getStatusText(appUpdate.status)}
                            </Typography>
                        </Box>
                    )}

                {appUpdate.status === "Failed" && (
                    <Typography
                        variant="body2"
                        color="error"
                        sx={{ mt: 2 }}
                    >
                        {appUpdate.error ?? "Oppdateringen feilet."}
                    </Typography>
                )}

                {canUpdate && (
                    <Button
                        variant="contained"
                        sx={{ mt: 2 }}
                        onClick={requestUpdate}
                    >
                        Oppdater til {version.kammichGithubVersion}
                    </Button>
                )}
            </Box>

            <MadeInNorwayBadge />
        </Box>
    );
}

function getStatusText(status: AppUpdateProgress["status"]) {
    switch (status) {
        case "Checking":
            return "Sjekker etter oppdateringer..."

        case "Verifying":
            return "Verifiserer oppdatering..."

        case "Replacing":
            return "Installerer oppdatering..."

        case "Restarting":
            return "Starter Kammich på nytt..."

        default:
            return null
    }
}

