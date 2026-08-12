import { useState } from "react";
import {
    Box, Typography, Paper, Stack,
    Accordion, AccordionSummary, AccordionDetails, Collapse,
    IconButton, Chip, TextField, CircularProgress, Button
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import HistoryIcon from '@mui/icons-material/History';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import type { DeviceImportJobSummary } from "../../types/types";
import { getHistoricalImportFiles } from "../../api/requests/importer";
import { claimImportJob } from "../../api/requests/claim";

export function ImportHistoryList({ history, historyLoaded, onFetchHistory }: {
    history: Array<DeviceImportJobSummary>;
    historyLoaded: boolean;
    onFetchHistory: () => void;
}) {
    const [searchQuery, setSearchQuery] = useState("");
    const [expandedJobId, setExpandedJobId] = useState<string | null>(null);

    const [jobFilesMap, setJobFilesMap] = useState<Record<string, Array<string>>>({});
    const [loadingJobId, setLoadingJobId] = useState<string | null>(null);
    const [claimingJobId, setClaimingJobId] = useState<string | null>(null);

    const handleToggleExpand = async (jobId: string) => {
        if (expandedJobId === jobId) {
            setExpandedJobId(null);
            return;
        }

        setExpandedJobId(jobId);

        if (!jobFilesMap[jobId]) {
            try {
                setLoadingJobId(jobId);
                const files = await getHistoricalImportFiles(jobId);
                setJobFilesMap(prev => ({ ...prev, [jobId]: files }));
            } catch (err) {
                console.error("Feil under henting av filer for jobb:", jobId, err);
            } finally {
                setLoadingJobId(null);
            }
        }
    };

    const handleClaim = async (jobId: string, e: React.MouseEvent) => {
        e.stopPropagation(); // Hindrer at accordion/ekspandering trigges unødvendig
        try {
            setClaimingJobId(jobId);
            await claimImportJob(jobId);
            // Hent historikken på nytt for å oppdatere eierskap/status i hele listen
            onFetchHistory();
        } catch (err) {
            console.error("Feil under claiming av jobb:", jobId, err);
        } finally {
            setClaimingJobId(null);
        }
    };

    return (
        <Accordion onChange={(_, expanded) => { if (expanded && !historyLoaded) onFetchHistory(); }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Stack sx={{ flexDirection: "row", gap: 1, alignItems: "center" }}>
                    <HistoryIcon />
                    <Typography variant="h6">Tidligere import-historikk</Typography>
                </Stack>
            </AccordionSummary>
            <AccordionDetails>
                <Box sx={{ mb: 2 }}>
                    <TextField
                        size="small"
                        fullWidth
                        placeholder="Søk i enhet..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </Box>

                {history.length === 0 ? (
                    <Typography color="text.secondary">Ingen historikk funnet.</Typography>
                ) : (
                    <Stack sx={{ gap: 1.5 }}>
                        {history.map((deviceGroup, devIndex) => {
                            const matchesDeviceSearch = searchQuery === "" ||
                                deviceGroup.deviceId.toLowerCase().includes(searchQuery.toLowerCase()) ||
                                deviceGroup.deviceName.toLowerCase().includes(searchQuery.toLowerCase());

                            if (!matchesDeviceSearch) return null;

                            return (
                                <Box key={devIndex} sx={{ mb: 2 }}>
                                    <Typography variant="subtitle1" sx={{ fontWeight: "bold", mb: 1 }}>
                                        {deviceGroup.deviceName} <Typography component="span" variant="caption" color="text.secondary">({deviceGroup.deviceId})</Typography>
                                    </Typography>

                                    <Stack sx={{ gap: 1 }}>
                                        {deviceGroup.jobs.map((job) => {
                                            const isExpanded = expandedJobId === job.jobId;
                                            const files = jobFilesMap[job.jobId] || [];
                                            const isLoadingFiles = loadingJobId === job.jobId;
                                            const isClaiming = claimingJobId === job.jobId;

                                            return (
                                                <Paper key={job.jobId} variant="outlined" sx={{ p: 2 }}>
                                                    <Stack sx={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                                                        <Box>
                                                            <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                                                                Jobb-ID: {job.jobId.substring(0, 8)}...
                                                            </Typography>
                                                            <Typography variant="caption" color="text.secondary">
                                                                Filer: {job.completedFiles}/{job.totalFiles}
                                                            </Typography>
                                                        </Box>
                                                        <Stack sx={{ flexDirection: "row", gap: 1, alignItems: "center" }}>
                                                            {/* Vis Claime-knapp KUN hvis den faktisk er claimable */}
                                                            {job.claimable ? (
                                                                <Button
                                                                    variant="contained"
                                                                    size="small"
                                                                    disabled={isClaiming}
                                                                    onClick={(e) => handleClaim(job.jobId, e)}
                                                                    sx={{ height: 24, fontSize: "0.75rem", textTransform: "none" }}
                                                                >
                                                                    {isClaiming ? <CircularProgress size={14} color="inherit" /> : "Claime"}
                                                                </Button>
                                                            ) : job.claimedBy ? (
                                                                // Hvis den ikke er claimable fordi den allerede er tatt
                                                                <Chip
                                                                    icon={<CheckCircleIcon fontSize="small" />}
                                                                    label="Claimet"
                                                                    color="success"
                                                                    size="small"
                                                                    variant="outlined"
                                                                />
                                                            ) : (
                                                                <></>
                                                            )}

                                                            <Chip
                                                                label={job.completedFiles >= job.totalFiles ? "Fullført" : "Pågår/Feilet"}
                                                                color={job.completedFiles >= job.totalFiles ? "success" : "warning"}
                                                                size="small"
                                                            />
                                                            <IconButton
                                                                size="small"
                                                                onClick={() => handleToggleExpand(job.jobId)}
                                                                title="Vis filer"
                                                            >
                                                                <ExpandMoreIcon sx={{ transform: isExpanded ? "rotate(180deg)" : "none", transition: "0.2s" }} />
                                                            </IconButton>
                                                        </Stack>
                                                    </Stack>

                                                    {/* Utvidet visning som henter filer on-demand */}
                                                    <Collapse in={isExpanded}>
                                                        <Box sx={{ mt: 2, pt: 2, borderTop: "1px solid", borderColor: "divider", maxHeight: 200, overflowY: "auto" }}>
                                                            <Typography variant="caption" sx={{ fontWeight: "bold", color: "text.secondary", display: "block", mb: 1 }}>
                                                                FILER I DENNE JOBBEN:
                                                            </Typography>

                                                            {isLoadingFiles ? (
                                                                <Stack sx={{ alignItems: "center", py: 2 }}>
                                                                    <CircularProgress size={24} />
                                                                </Stack>
                                                            ) : files.length === 0 ? (
                                                                <Typography variant="body2" color="text.secondary">Ingen filer funnet for denne jobben.</Typography>
                                                            ) : (
                                                                <Stack sx={{ gap: 0.5 }}>
                                                                    {files.map((fileName, fIdx) => (
                                                                        <Stack key={fIdx} sx={{ flexDirection: "row", alignItems: "center", gap: 1, p: 0.5 }}>
                                                                            <InsertDriveFileIcon fontSize="small" color="action" />
                                                                            <Typography variant="body2" sx={{ flexGrow: 1 }} noWrap>
                                                                                {fileName}
                                                                            </Typography>
                                                                        </Stack>
                                                                    ))}
                                                                </Stack>
                                                            )}
                                                        </Box>
                                                    </Collapse>
                                                </Paper>
                                            );
                                        })}
                                    </Stack>
                                </Box>
                            );
                        })}
                    </Stack>
                )}
            </AccordionDetails>
        </Accordion>
    );
}