import { Box, Typography, Paper, Chip, Stack, CircularProgress, LinearProgress } from "@mui/material";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import CodeIcon from "@mui/icons-material/Code";
import StorageIcon from "@mui/icons-material/Storage";
import SettingsIcon from "@mui/icons-material/Settings";
import ImmichLogin from "../components/immich/ImmichLogin";
import { useSseSelector } from "../sse/useSseSelector";
import { useEffect, useState } from "react";
import type { ImmichServerConnection, ImmichServerConfig, ImmichServerFeatures, ImmichServerStorage, ImmichSupportedMediaTypes } from "../types/types";
import { immichConfig, immichFeatures, immichMediaTypes, immichStorage, immichUrl, immichVersion } from "../api/requests/immich";

// --- Underkomponenter for hvert enkelt kort for renere kode ---

function ServerInfoCard({ config, version, userName, url }: { config: ImmichServerConfig | null; version: string | null; userName?: string, url?: string | null }) {
    return (
        <Paper
            elevation={0}
            sx={{
                p: 3,
                borderRadius: 4,
                bgcolor: "background.paper",
                border: "1px solid",
                borderColor: "divider",
                display: "flex",
                flexDirection: "column",
                gap: 2,
                height: "100%"
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                <InfoOutlinedIcon color="primary" />
                <Typography variant="h6" sx={{ fontWeight: "600" }}>
                    Serverinformasjon
                </Typography>
            </Box>

            <Stack spacing={1.5} sx={{ mt: 1 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <Typography variant="body2" color="text.secondary">Status</Typography>
                    <Chip
                        icon={<CheckCircleOutlineIcon style={{ color: "lime" }} />}
                        label={config?.maintenanceMode ? "Vedlikeholdsmodus" : "Tilkoblet"}
                        size="small"
                        variant="outlined"
                        sx={{ borderColor: "divider" }}
                    />
                </Box>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Versjon</Typography>
                    <Typography variant="body2" sx={{ fontWeight: "medium" }}>{version || "Ukjent"}</Typography>
                </Box>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Nåværende endepunkt</Typography>
                    <Typography variant="body2" sx={{ fontWeight: "medium" }}>{url || "Ikke satt"}</Typography>
                </Box>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Eksternt domene</Typography>
                    <Typography variant="body2" sx={{ fontWeight: "medium" }}>{config?.externalDomain || "Ikke satt"}</Typography>
                </Box>
                {userName && (
                    <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                        <Typography variant="body2" color="text.secondary">Innlogget som</Typography>
                        <Typography variant="body2" sx={{ fontWeight: "medium" }}>{userName}</Typography>
                    </Box>
                )}
            </Stack>
        </Paper>
    );
}

function StorageCard({ storage }: { storage: ImmichServerStorage }) {
    const percentage = Math.min(storage.diskUsagePercentage, 100);

    return (
        <Paper
            elevation={0}
            sx={{
                p: 3,
                borderRadius: 4,
                bgcolor: "background.paper",
                border: "1px solid",
                borderColor: "divider",
                display: "flex",
                flexDirection: "column",
                gap: 2,
                height: "100%"
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                <StorageIcon color="primary" />
                <Typography variant="h6" sx={{ fontWeight: "600" }}>
                    Lagringsplass
                </Typography>
            </Box>

            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mt: 1 }}>
                <Typography variant="body2" color="text.secondary">Brukt</Typography>
                <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                    {storage.diskUse} / {storage.diskSize}
                </Typography>
            </Box>

            <LinearProgress
                variant="determinate"
                value={percentage}
                sx={{ height: 8, borderRadius: 4 }}
            />

            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <Typography variant="caption" color="text.secondary">
                    Ledig: {storage.diskAvailable}
                </Typography>
                <Typography variant="caption" sx={{ fontWeight: "600" }} color="text.primary">
                    {storage.diskUsagePercentage.toFixed(1)}% brukt
                </Typography>
            </Box>
        </Paper>
    );
}

function MediaTypesCard({ mediaTypes }: { mediaTypes: ImmichSupportedMediaTypes }) {
    return (
        <Paper
            elevation={0}
            sx={{
                p: 3,
                borderRadius: 4,
                bgcolor: "background.paper",
                border: "1px solid",
                borderColor: "divider",
                display: "flex",
                flexDirection: "column",
                gap: 2,
                height: "100%"
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                <CodeIcon color="secondary" />
                <Typography variant="h6" sx={{ fontWeight: "600" }}>
                    Støttede formater
                </Typography>
            </Box>

            <Typography variant="caption" color="text.secondary">
                Bilde- og videoformater serveren håndterer:
            </Typography>

            <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5, mt: 1 }}>
                <Box>
                    <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 0.5 }}>Bilder:</Typography>
                    <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.75 }}>
                        {mediaTypes.images.map((format) => (
                            <Chip key={format} label={format} size="small" sx={{ bgcolor: "action.hover", fontWeight: 500 }} />
                        ))}
                    </Box>
                </Box>
                <Box>
                    <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 0.5 }}>Videoer:</Typography>
                    <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.75 }}>
                        {mediaTypes.videos.map((format) => (
                            <Chip key={format} label={format} size="small" sx={{ bgcolor: "action.hover", fontWeight: 500 }} />
                        ))}
                    </Box>
                </Box>
            </Box>
        </Paper>
    );
}

function ServerFeaturesCard({ features }: { features: ImmichServerFeatures }) {
    return (
        <Paper
            elevation={0}
            sx={{
                p: 3,
                borderRadius: 4,
                bgcolor: "background.paper",
                border: "1px solid",
                borderColor: "divider",
                display: "flex",
                flexDirection: "column",
                gap: 2,
                height: "100%"
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                <SettingsIcon color="action" />
                <Typography variant="h6" sx={{ fontWeight: "600" }}>
                    Serverfunksjoner
                </Typography>
            </Box>

            <Box sx={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: 1.5, mt: 1 }}>
                {Object.entries(features).map(([key, enabled]) => {
                    const formattedKey = key
                        .replace(/Enabled|Supported|Available/, "")
                        .replace(/([A-Z])/g, " $1")
                        .trim();

                    return (
                        <Box key={key} sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                            <Box
                                sx={{
                                    width: 8,
                                    height: 8,
                                    borderRadius: "50%",
                                    bgcolor: enabled ? "success.main" : "text.disabled"
                                }}
                            />
                            <Typography
                                variant="body2"
                                sx={{
                                    color: enabled ? "text.primary" : "text.disabled",
                                    textTransform: "capitalize",
                                    fontSize: "0.85rem"
                                }}
                            >
                                {formattedKey}
                            </Typography>
                        </Box>
                    );
                })}
            </Box>
        </Paper>
    );
}


export function ImmichLoginComponent() {
    return (
        <Box
            sx={{
                minHeight: "100%",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                bgcolor: "#0d0d0d",
                p: 2,
                overflowY: "auto",
            }}
        >
            <ImmichLogin />
        </Box>
    )
}

// --- Hovedkomponent ---

export default function Immich() {
    const immichUser = useSseSelector(state => state.immichUserMe);

    const [version, setVersion] = useState<string | null>(null);
    const [mediaTypes, setMediaTypes] = useState<ImmichSupportedMediaTypes | null>(null);
    const [features, setFeatures] = useState<ImmichServerFeatures | null>(null);
    const [config, setConfig] = useState<ImmichServerConfig | null>(null);
    const [storage, setStorage] = useState<ImmichServerStorage | null>(null);
    const [connectionUrl, setConnectionUrl] = useState<ImmichServerConnection | null>(null)

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function fetchServerData() {
            try {
                setLoading(true);

                // 1. Endre hvordan du henter og formaterer versjonen i useEffect / fetchServerData:
                const vRes = await immichVersion().catch(err => {
                    console.error("Feil på version:", err);
                    return null;
                });

                // Bygg en pen versjonstreng fra objektet, f.eks. "1.115.0"
                const formattedVersion = vRes
                    ? `${vRes.major}.${vRes.minor}.${vRes.patch}${vRes.preRelease ? `-${vRes.preRelease}` : ""}`
                    : "Ukjent";

                setVersion(formattedVersion);
                const mt = await immichMediaTypes().catch(() => null);
                const f = await immichFeatures().catch(() => null);
                const c = await immichConfig().catch(() => null);
                const s = await immichStorage().catch(() => null);
                const url = await immichUrl().catch(() => null);

                setVersion(formattedVersion ?? "Ukjent");
                setMediaTypes(mt);
                setFeatures(f);
                setConfig(c);
                setStorage(s);
                setConnectionUrl(url);
            } catch (err: any) {
                setError(err?.message || "Kunne ikke hente serverinformasjon fra Immich.");
            } finally {
                setLoading(false);
            }
        }

        if (immichUser) {
            fetchServerData();
        }

    }, []);

    if (!immichUser) return <ImmichLoginComponent />;

    return (
        <Box sx={{ p: { xs: 2, sm: 4 }, bgcolor: "background.default", minHeight: "100%", maxWidth: 1100, mx: "auto" }}>
            <Typography variant="h4" sx={{ mb: 1, fontWeight: 700, color: "text.primary" }}>
                Immich Dashboard
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
                Oversikt over tilkoblet Immich-server, versjon og systemegenskaper.
            </Typography>

            {loading ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
                    <CircularProgress />
                </Box>
            ) : error ? (
                <Paper
                    elevation={0}
                    sx={{ p: 3, borderRadius: 4, bgcolor: "error.main", color: "error.contrastText", border: "1px solid", borderColor: "error.dark" }}
                >
                    <Typography variant="body2">{error}</Typography>
                </Paper>
            ) : (
                <Box
                    sx={{
                        display: "grid",
                        gridTemplateColumns: { xs: "1fr", md: "repeat(2, 1fr)" },
                        gap: 3,
                        alignItems: "start" // Sikrer at kort med ulik høyde flyter pent og stabilt ved siden av hverandre
                    }}
                >
                    <ServerInfoCard config={config} version={version} userName={immichUser.name} url={connectionUrl?.url} />
                    {storage && <StorageCard storage={storage} />}
                    {mediaTypes && <MediaTypesCard mediaTypes={mediaTypes} />}
                    {features && <ServerFeaturesCard features={features} />}
                </Box>
            )}
        </Box>
    );
}