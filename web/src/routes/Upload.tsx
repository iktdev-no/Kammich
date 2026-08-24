import React, { useEffect, useState } from "react";
import {
    Box,
    Container,
    Typography,
    Grid,
    Card,
    CardContent,
    Button,
    IconButton,
    Tooltip,
    LinearProgress,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import PhotoAlbumIcon from '@mui/icons-material/PhotoAlbum';
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutlined";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutlined";
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty";
import { useSseSelector } from "../sse/useSseSelector";
import type { UploadSummary, UploadJobSummary } from "../types/types";
import { getJobs, getStats, resetJobQueue, resetUserQueue } from "../api/requests/upload";

export default function Home() {
    const immichUser = useSseSelector((state) => state.immichUserMe);
    const activeUploadProgress = useSseSelector((state) => state.activeUploadProgress);

    const [stats, setStats] = useState<UploadSummary | null>(null);
    const [jobs, setJobs] = useState<UploadJobSummary[]>([]);
    const [loading, setLoading] = useState<boolean>(false);

    const userId = immichUser?.id;

    const fetchData = async () => {
        if (!userId) return;
        try {
            setLoading(true);
            const [statsRes, jobsRes] = await Promise.all([
                getStats(userId),
                getJobs(userId),
            ]);
            setStats(statsRes);
            setJobs(jobsRes);
        } catch (err) {
            console.error("Klarte ikke å hente opplastingsdata", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (userId) {
            fetchData();
        }
    }, [userId]);

    const handleResetUser = async () => {
        if (!userId) return;
        await resetUserQueue(userId);
        fetchData();
    };

    const handleResetJob = async (jobId: string) => {
        if (!userId) return;
        await resetJobQueue(userId, jobId);
        fetchData();
    };

    if (!userId) {
        return (
            <Box sx={{ p: 4 }}>
                <Typography>Vennligst logg inn på immich først</Typography>
            </Box>
        );
    }

    return (
        <Box sx={{ minHeight: "100vh", py: { xs: 2, md: 4 }, px: { xs: 1, md: 2 } }}>
            <Container maxWidth="lg" sx={{ px: { xs: 1, sm: 2 } }}>

                {/* Header */}
                <Box
                    sx={{
                        display: "flex",
                        flexDirection: { xs: "column", sm: "row" },
                        justifyContent: "space-between",
                        alignItems: { xs: "flex-start", sm: "center" },
                        gap: 2,
                        mb: 4,
                    }}
                >
                    <Box>
                        <Typography variant="h5" color="textPrimary" sx={{ fontSize: { xs: "1.25rem", sm: "1.5rem" } }}>
                            Opplastingskontroll
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                            Sanntidsovervåking av filer og jobber på vei til Immich (Bruker: {immichUser.name || userId})
                        </Typography>
                    </Box>
                    <Button
                        variant="outlined"
                        size="small"
                        startIcon={<RefreshIcon />}
                        onClick={fetchData}
                        disabled={loading}
                        sx={{ borderColor: "rgba(255,255,255,0.12)", color: "text.primary", alignSelf: { xs: "stretch", sm: "auto" } }}
                    >
                        Oppdater
                    </Button>
                </Box>

                {/* Statistikk-kort */}
                <UploadStats stats={stats} />

                {/* Handlingsknapper */}
                <Box sx={{ display: "flex", justifyContent: "flex-end", mb: 3 }}>
                    <Button
                        variant="contained"
                        color="error"
                        size="small"
                        onClick={handleResetUser}
                        sx={{ width: { xs: "100%", sm: "auto" } }}
                    >
                        Nullstill alle feilede for bruker
                    </Button>
                </Box>

                {/* Jobber som kort */}
                <Typography variant="h6" sx={{ mb: 2, fontSize: "1.1rem" }}>
                    Aktive og tidligere jobber
                </Typography>

                {jobs.length === 0 ? (
                    <Card sx={{ backgroundColor: "background.paper", borderRadius: 2, border: "1px solid rgba(255,255,255,0.06)", p: 4, textAlign: "center" }}>
                        <Typography color="textSecondary">Ingen aktive eller tidligere opplastingsjobber funnet.</Typography>
                    </Card>
                ) : (
                    <Grid container spacing={2}>
                        {jobs.map((job) => {
                            const liveProgress = activeUploadProgress[job.jobId];

                            const total = liveProgress ? liveProgress.totalFiles : job.total;
                            const success = liveProgress ? liveProgress.successfulFiles : job.totalSuccess;
                            const failed = liveProgress ? liveProgress.failedFiles : job.totalFailure;

                            const processed = success + failed;
                            const progressPercent = total > 0 ? (processed / total) * 100 : 0;

                            // Vis kun reset-knapp dersom jobben ikke er helt ferdig (totalt avvik fra suksess)
                            const showResetButton = total !== success;

                            return (
                                <Grid size={{ xs: 12, md: 6 }} key={job.jobId}>
                                    <Card sx={{ backgroundColor: "background.paper", borderRadius: 2, border: "1px solid rgba(255,255,255,0.06)", height: "100%", display: "flex", flexDirection: "column" }}>
                                        <CardContent sx={{ p: 2.5, flexGrow: 1, display: "flex", flexDirection: "column", justifyContent: "space-between" }}>

                                            {/* Topptekst med Jobb ID og betinget restart-knapp */}
                                            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", mb: 2 }}>
                                                <Box>
                                                    <Typography variant="caption" color="textSecondary" sx={{ display: "block" }} >
                                                        Jobb ID
                                                    </Typography>
                                                    <Typography variant="body2" sx={{ fontFamily: "monospace", fontWeight: 600, wordBreak: "break-all" }}>
                                                        {job.jobId}
                                                    </Typography>
                                                </Box>
                                                {showResetButton && (
                                                    <Tooltip title="Nullstill og kjør jobb på nytt">
                                                        <IconButton size="small" onClick={() => handleResetJob(job.jobId)} color="primary" sx={{ ml: 1 }}>
                                                            <RefreshIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}
                                            </Box>

                                            {/* Statistikk for jobben */}
                                            <Box sx={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 1, mb: 2, backgroundColor: "rgba(255,255,255,0.02)", p: 1.5, borderRadius: 1.5 }}>
                                                <Box>
                                                    <Typography variant="caption" color="textSecondary">Totalt</Typography>
                                                    <Typography variant="body1" sx={{ fontWeight: 600 }}>{total}</Typography>
                                                </Box>
                                                <Box>
                                                    <Typography variant="caption" color="textSecondary">Suksess</Typography>
                                                    <Typography variant="body1" sx={{ fontWeight: 600 }} color="success.main">{success}</Typography>
                                                </Box>
                                                <Box>
                                                    <Typography variant="caption" color="textSecondary">Feilet</Typography>
                                                    <Typography variant="body1" sx={{ fontWeight: 600 }} color={failed > 0 ? "error.main" : "inherit"}>{failed}</Typography>
                                                </Box>
                                            </Box>

                                            {/* Progresjonslinje i bunn */}
                                            <Box>
                                                <Box sx={{ display: "flex", justifyContent: "space-between", mb: 0.5 }}>
                                                    <Typography variant="caption" color="textSecondary">Fremdrift ({processed} / {total})</Typography>
                                                    <Typography variant="caption" sx={{ fontWeight: 600 }} color="textSecondary">
                                                        {Math.round(progressPercent)}%
                                                    </Typography>
                                                </Box>
                                                <LinearProgress
                                                    variant="determinate"
                                                    value={progressPercent}
                                                    sx={{ borderRadius: 4, height: 6, backgroundColor: "rgba(255,255,255,0.08)" }}
                                                />
                                            </Box>

                                        </CardContent>
                                    </Card>
                                </Grid>
                            );
                        })}
                    </Grid>
                )}

            </Container>
        </Box>
    );
}

function UploadStats({ stats }: { stats: UploadSummary | null }) {
    return (
        <Grid container spacing={1} sx={{ mb: 4 }}>
            <Grid size={{ xs: 3 }}>
                <CardStat label="Total" count={stats?.totalUploads ?? 0} color="textSecondary" icon={<PhotoAlbumIcon color="primary" fontSize="small" />} />
            </Grid>
            <Grid size={{ xs: 3 }}>
                <CardStat label="Pending" count={stats?.totalReadyUploads ?? 0} color="warning.main" icon={<HourglassEmptyIcon color="warning" fontSize="small" />} />
            </Grid>
            <Grid size={{ xs: 3 }}>
                <CardStat label="Succeeded" count={stats?.totalSucceededUploads ?? 0} color="success.main" icon={<CheckCircleOutlineIcon color="success" fontSize="small" />} />
            </Grid>
            <Grid size={{ xs: 3 }}>
                <CardStat label="Failed" count={stats?.totalFailedUploads ?? 0} color="error.main" icon={<ErrorOutlineIcon color="error" fontSize="small" />} />
            </Grid>
        </Grid>
    );
}

interface CardStatProps {
    icon: React.ReactNode;
    count: number;
    label: string;
    color: string;
}

export function CardStat({ icon, count, label, color }: CardStatProps) {
    return (
        <Card sx={{ backgroundColor: "background.paper", borderRadius: 2, border: "1px solid rgba(255,255,255,0.06)", height: "100%" }}>
            <CardContent sx={{ p: { xs: 1.5, sm: 2 }, "&:last-child": { pb: { xs: 1.5, sm: 2 } } }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 0.5 }}>
                    {icon}
                    <Typography color="textSecondary" variant="subtitle2" sx={{ fontSize: { xs: "0.7rem", sm: "0.875rem" }, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                        {label}
                    </Typography>
                </Box>
                <Typography variant="h5" sx={{ fontSize: { xs: "1.2rem", sm: "1.75rem" }, fontWeight: 600 }} color={color}>
                    {count}
                </Typography>
            </CardContent>
        </Card>
    );
}