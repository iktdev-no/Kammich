import React, { useEffect, useState } from "react";
import {
    Box,
    Container,
    Typography,
    Grid,
    Card,
    CardContent,
    Button,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    IconButton,
    Tooltip,
    LinearProgress,
    createTheme,
    ThemeProvider,
    CssBaseline,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutlined";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutlined";
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty";
import { useSseSelector } from "../sse/useSseSelector";
import type { UploadSummary, UploadJobSummary } from "../types/types";
import { getJobs, getStats, resetJobQueue, resetUserQueue } from "../api/requests/upload";

// Immich-inspirert mørkt tema
const immichDarkTheme = createTheme({
    palette: {
        mode: "dark",
        background: {
            default: "#121214",
            paper: "#1a1a1e",
        },
        primary: {
            main: "#4285f4",
        },
        success: {
            main: "#34a853",
        },
        error: {
            main: "#ea4335",
        },
        warning: {
            main: "#fbbc05",
        },
        text: {
            primary: "#e8eaed",
            secondary: "#9aa0a6",
        },
    },
    shape: {
        borderRadius: 12,
    },
    typography: {
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
        h5: {
            fontWeight: 600,
        },
        h6: {
            fontWeight: 600,
        },
    },
});

export default function Home() {
    const ping = useSseSelector((state) => state.lastPing);
    const immichUser = useSseSelector((state) => state.immichUserMe);
    const uploadProgress = useSseSelector((state) => state.activeUploadProgress);

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
            <ThemeProvider theme={immichDarkTheme}>
                <CssBaseline />
                <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
                    <Typography color="textSecondary">Venter på Immich-bruker...</Typography>
                </Box>
            </ThemeProvider>
        );
    }

    return (
        <ThemeProvider theme={immichDarkTheme}>
            <CssBaseline />
            <Box sx={{ minHeight: "100vh", py: 4, px: 2 }}>
                <Container maxWidth="lg">

                    {/* Header */}
                    <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                        <Box>
                            <Typography variant="h5" color="textPrimary">
                                Opplastingskontroll
                            </Typography>
                            <Typography variant="body2" color="textSecondary">
                                Sanntidsovervåking av filer og jobber på vei til Immich (Bruker: {immichUser.name || userId})
                            </Typography>
                        </Box>
                        <Box display="flex" gap={2} alignItems="center">
                            <Typography variant="caption" sx={{ color: "text.secondary" }}>
                                SSE Ping: {ping || "Venter..."}
                            </Typography>
                            <Button
                                variant="outlined"
                                size="small"
                                startIcon={<RefreshIcon />}
                                onClick={fetchData}
                                disabled={loading}
                                sx={{ borderColor: "rgba(255,255,255,0.12)", color: "text.primary" }}
                            >
                                Oppdater
                            </Button>
                        </Box>
                    </Box>

                    {/* Statistikk-kort (MUI v6 Grid-syntaks uten 'item') */}
                    {stats && (
                        <Grid container spacing={3} sx={{ mb: 4 }}>
                            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                                <Card sx={{ backgroundColor: "background.paper", border: "1px solid rgba(255,255,255,0.06)" }}>
                                    <CardContent>
                                        <Typography color="textSecondary" variant="subtitle2" gutterBottom>
                                            Totalt Filer
                                        </Typography>
                                        <Typography variant="h4">{stats.totalUploads}</Typography>
                                    </CardContent>
                                </Card>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                                <Card sx={{ backgroundColor: "background.paper", border: "1px solid rgba(255,255,255,0.06)" }}>
                                    <CardContent>
                                        <Box display="flex" alignItems="center" gap={1} mb={1}>
                                            <HourglassEmptyIcon color="warning" fontSize="small" />
                                            <Typography color="textSecondary" variant="subtitle2">
                                                Klar / Venter
                                            </Typography>
                                        </Box>
                                        <Typography variant="h4">{stats.totalReadyUploads}</Typography>
                                    </CardContent>
                                </Card>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                                <Card sx={{ backgroundColor: "background.paper", border: "1px solid rgba(255,255,255,0.06)" }}>
                                    <CardContent>
                                        <Box display="flex" alignItems="center" gap={1} mb={1}>
                                            <CheckCircleOutlineIcon color="success" fontSize="small" />
                                            <Typography color="textSecondary" variant="subtitle2">
                                                Fullført
                                            </Typography>
                                        </Box>
                                        <Typography variant="h4" color="success.main">
                                            {stats.totalSucceededUploads}
                                        </Typography>
                                    </CardContent>
                                </Card>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                                <Card sx={{ backgroundColor: "background.paper", border: "1px solid rgba(255,255,255,0.06)" }}>
                                    <CardContent>
                                        <Box display="flex" alignItems="center" gap={1} mb={1}>
                                            <ErrorOutlineIcon color="error" fontSize="small" />
                                            <Typography color="textSecondary" variant="subtitle2">
                                                Feilet
                                            </Typography>
                                        </Box>
                                        <Typography variant="h4" color="error.main">
                                            {stats.totalFailedUploads}
                                        </Typography>
                                    </CardContent>
                                </Card>
                            </Grid>
                        </Grid>
                    )}

                    {/* Handlingsknapper */}
                    <Box display="flex" justifyContent="flex-end" mb={2}>
                        <Button
                            variant="contained"
                            color="error"
                            size="small"
                            onClick={handleResetUser}
                        >
                            Nullstill alle feilede for bruker
                        </Button>
                    </Box>

                    {/* Jobb-tabell */}
                    <TableContainer component={Paper} sx={{ backgroundColor: "background.paper", border: "1px solid rgba(255,255,255,0.06)" }}>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell sx={{ fontWeight: 600 }}>Jobb ID</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 600 }}>Totalt</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 600 }}>Suksess</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 600 }}>Feilet</TableCell>
                                    <TableCell align="center" sx={{ fontWeight: 600 }}>Fremdrift</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 600 }}>Handlinger</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {jobs.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={6} align="center" sx={{ py: 4, color: "text.secondary" }}>
                                            Ingen aktive eller tidligere opplastingsjobber funnet.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    jobs.map((job) => {
                                        const progress = job.total > 0 ? ((job.totalSuccess + job.totalFailure) / job.total) * 100 : 0;
                                        return (
                                            <TableRow key={job.jobId} hover sx={{ "&:last-child td, &:last-child th": { border: 0 } }}>
                                                <TableCell component="th" scope="row" sx={{ fontFamily: "monospace", fontSize: "0.85rem" }}>
                                                    {job.jobId}
                                                </TableCell>
                                                <TableCell align="right">{job.total}</TableCell>
                                                <TableCell align="right" sx={{ color: "success.main" }}>{job.totalSuccess}</TableCell>
                                                <TableCell align="right" sx={{ color: job.totalFailure > 0 ? "error.main" : "inherit" }}>
                                                    {job.totalFailure}
                                                </TableCell>
                                                <TableCell align="center" sx={{ width: "20%" }}>
                                                    <Box display="flex" alignItems="center" gap={1}>
                                                        <Box width="100%">
                                                            <LinearProgress variant="determinate" value={progress} sx={{ borderRadius: 4, height: 6 }} />
                                                        </Box>
                                                        <Typography variant="caption" color="textSecondary">
                                                            {Math.round(progress)}%
                                                        </Typography>
                                                    </Box>
                                                </TableCell>
                                                <TableCell align="right">
                                                    <Tooltip title="Nullstill og kjør jobb på nytt">
                                                        <IconButton size="small" onClick={() => handleResetJob(job.jobId)} color="primary">
                                                            <RefreshIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })
                                )}
                            </TableBody>
                        </Table>
                    </TableContainer>

                </Container>
            </Box>
        </ThemeProvider>
    );
}