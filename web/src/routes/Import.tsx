import { useEffect, useState } from "react";
import {
    Box, Typography, Paper, LinearProgress, Stack,
    Accordion, AccordionSummary, AccordionDetails, Collapse,
    IconButton, Chip, TextField
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CancelIcon from '@mui/icons-material/Cancel';
import DownloadDoneIcon from '@mui/icons-material/DownloadDone';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlineOutlined';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import CircularProgress from '@mui/material/CircularProgress';
import HistoryIcon from '@mui/icons-material/History';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import { useSseSelector } from "../sse/useSseSelector";
import type { DeviceImport, ImportProgressEvent } from "../types/types";
import { toast } from "react-toastify";
import { cancelImportFor, getHistoricalImports } from "../api/importer";

// --- UNDERKOMPONENT 1: Aktive importer ---
function ActiveImportsList({ activeImports, activeImportDevices, onCancel }: {
    activeImports: Array<ImportProgressEvent>;
    activeImportDevices: Record<string, any>;
    onCancel: (deviceId: string) => void;
}) {
    const [recentCompleted, setRecentCompleted] = useState<Array<ImportProgressEvent>>([]);

    useEffect(() => {
        activeImports.forEach(imp => {
            const isFinished = imp.completedFiles >= imp.totalFiles || imp.state === "Success";
            if (isFinished) {
                setRecentCompleted(prev => {
                    if (!prev.some(item => item.deviceId === imp.deviceId)) {
                        setTimeout(() => {
                            setRecentCompleted(curr => curr.filter(i => i.deviceId !== imp.deviceId));
                        }, 4000);
                        return [imp, ...prev].slice(0, 5);
                    }
                    return prev;
                });
            }
        });
    }, [activeImports]);

    if (activeImports.length === 0 && recentCompleted.length === 0) {
        return (
            <Paper sx={{ p: 3, textAlign: "center", color: "text.secondary", mb: 3 }}>
                Ingen aktive importer for øyeblikket.
            </Paper>
        );
    }

    return (
        <Stack sx={{ gap: 2, mb: 3 }}>
            {activeImports.map(imp => {
                const progress = imp.totalFiles > 0 ? (imp.completedFiles / imp.totalFiles) * 100 : 0;
                const summary = activeImportDevices[imp.deviceId];

                return (
                    <Paper key={imp.deviceId} variant="outlined" sx={{ p: 2, position: "relative" }}>
                        <Stack sx={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", mb: 1 }}>
                            <Box>
                                <Typography variant="subtitle1" sx={{ fontWeight: "bold" }}>
                                    Enhet: {imp.deviceId}
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    Laster ned: {imp.currentFile || "Forbereder..."} {summary ? `• Status: ${summary.state}` : ""}
                                </Typography>
                            </Box>
                            <Stack sx={{ flexDirection: "row", gap: 1, alignItems: "center" }}>
                                <Chip label={`${imp.completedFiles} / ${imp.totalFiles} filer`} size="small" color="primary" />
                                <IconButton size="small" color="error" onClick={() => onCancel(imp.deviceId)} title="Avbryt import">
                                    <CancelIcon />
                                </IconButton>
                            </Stack>
                        </Stack>

                        <LinearProgress variant="determinate" value={progress} sx={{ height: 8, borderRadius: 4, mb: 2 }} />

                        {imp.files && imp.files.length > 0 && (
                            <Box sx={{ bgcolor: "background.default", p: 1.5, borderRadius: 1.5 }}>
                                <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 1, fontWeight: "bold" }}>
                                    AKTIV KØ / FIL-VINDU
                                </Typography>
                                <Stack sx={{ gap: 0.75 }}>
                                    {imp.files.map((fileItem, idx) => {
                                        const isDone = fileItem.state === "Success";
                                        const isFailed = fileItem.state === "Failure";
                                        const isActive = fileItem.file === imp.currentFile || fileItem.state === "InProgress";

                                        return (
                                            <Stack key={`${fileItem.file}-${idx}`} sx={{ flexDirection: "row", alignItems: "center", gap: 1.5, p: 0.5 }}>
                                                {isDone ? (
                                                    <CheckCircleIcon color="success" fontSize="small" />
                                                ) : isFailed ? (
                                                    <ErrorOutlineIcon color="error" fontSize="small" />
                                                ) : isActive ? (
                                                    <CircularProgress size={16} color="primary" />
                                                ) : (
                                                    <RadioButtonUncheckedIcon color="disabled" fontSize="small" />
                                                )}
                                                <Typography
                                                    variant="body2"
                                                    sx={{
                                                        flexGrow: 1,
                                                        fontWeight: isActive ? "bold" : "normal",
                                                        color: isDone ? "text.secondary" : isFailed ? "error.main" : "text.primary",
                                                        textDecoration: isDone ? "line-through" : "none"
                                                    }}
                                                    noWrap
                                                >
                                                    {fileItem.file}
                                                </Typography>
                                                {fileItem.isNew && (
                                                    <Chip label="Ny" size="small" variant="outlined" sx={{ height: 18, fontSize: "0.6rem" }} />
                                                )}
                                            </Stack>
                                        );
                                    })}
                                </Stack>
                            </Box>
                        )}
                    </Paper>
                );
            })}

            {recentCompleted.map(imp => (
                <Collapse key={`recent-${imp.deviceId}`} in={true} timeout={600}>
                    <Paper variant="outlined" sx={{ p: 2, bgcolor: "action.hover", borderColor: "success.main" }}>
                        <Stack sx={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                            <Stack sx={{ flexDirection: "row", gap: 1.5, alignItems: "center" }}>
                                <DownloadDoneIcon color="success" />
                                <Box>
                                    <Typography variant="subtitle1" sx={{ fontWeight: "bold" }}>
                                        Enhet: {imp.deviceId} - Fullført!
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Importerte {imp.totalFiles} filer vellykket.
                                    </Typography>
                                </Box>
                            </Stack>
                            <Chip label="Ferdig" color="success" size="small" />
                        </Stack>
                    </Paper>
                </Collapse>
            ))}
        </Stack>
    );
}

// --- UNDERKOMPONENT 2: Historikk med filliste og søk ---
function ImportHistoryList({ history, historyLoaded, onFetchHistory }: {
    history: Array<DeviceImport>;
    historyLoaded: boolean;
    onFetchHistory: () => void;
}) {
    const [searchQuery, setSearchQuery] = useState("");
    const [expandedItemIndex, setExpandedItemIndex] = useState<number | null>(null);

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
                        placeholder="Søk i filnavn eller enhet..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </Box>

                {history.length === 0 ? (
                    <Typography color="text.secondary">Ingen historikk funnet.</Typography>
                ) : (
                    <Stack sx={{ gap: 1.5 }}>
                        {history.map((item, index) => {
                            // Filtrer filer hvis søk er aktivt
                            const filteredFiles = item.files?.filter(f =>
                                f.file.toLowerCase().includes(searchQuery.toLowerCase()) ||
                                item.deviceId.toLowerCase().includes(searchQuery.toLowerCase())
                            ) || [];

                            // Hvis brukeren søker, og ingen filer matcher, kan vi hoppe over denne historikk-posten med mindre enhets-ID matcher
                            const matchesSearch = searchQuery === "" ||
                                item.deviceId.toLowerCase().includes(searchQuery.toLowerCase()) ||
                                filteredFiles.length > 0;

                            if (!matchesSearch) return null;

                            const isItemExpanded = expandedItemIndex === index;

                            return (
                                <Paper key={index} variant="outlined" sx={{ p: 2 }}>
                                    <Stack sx={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                                        <Box>
                                            <Typography variant="subtitle2">Enhet: {item.deviceId}</Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                Startet: {new Date(item.started).toLocaleString()} • Filer: {item.completedFiles}/{item.totalFiles}
                                            </Typography>
                                        </Box>
                                        <Stack sx={{ flexDirection: "row", gap: 1, alignItems: "center" }}>
                                            <Chip
                                                label={item.completedFiles >= item.totalFiles ? "Fullført" : "Avbrutt/Feilet"}
                                                color={item.completedFiles >= item.totalFiles ? "success" : "warning"}
                                                size="small"
                                            />
                                            <IconButton
                                                size="small"
                                                onClick={() => setExpandedItemIndex(isItemExpanded ? null : index)}
                                                title="Vis filer"
                                            >
                                                <ExpandMoreIcon sx={{ transform: isItemExpanded ? "rotate(180deg)" : "none", transition: "0.2s" }} />
                                            </IconButton>
                                        </Stack>
                                    </Stack>

                                    {/* Utvidet visning av filer for denne historikk-økten */}
                                    <Collapse in={isItemExpanded}>
                                        <Box sx={{ mt: 2, pt: 2, borderTop: "1px solid", borderColor: "divider", maxHeight: 200, overflowY: "auto" }}>
                                            <Typography variant="caption" sx={{ fontWeight: "bold", color: "text.secondary", display: "block", mb: 1 }}>
                                                FILER I DENNE ØKTEN ({item.files?.length || 0}):
                                            </Typography>
                                            <Stack sx={{ gap: 0.5 }}>
                                                {item.files?.map((f, fIdx) => (
                                                    <Stack key={fIdx} sx={{ flexDirection: "row", alignItems: "center", gap: 1, p: 0.5 }}>
                                                        <InsertDriveFileIcon fontSize="small" color="action" />
                                                        <Typography variant="body2" sx={{ flexGrow: 1 }} noWrap>
                                                            {f.file}
                                                        </Typography>
                                                        <Chip label={f.state} size="small" variant="outlined" sx={{ height: 18, fontSize: "0.6rem" }} />
                                                    </Stack>
                                                ))}
                                            </Stack>
                                        </Box>
                                    </Collapse>
                                </Paper>
                            );
                        })}
                    </Stack>
                )}
            </AccordionDetails>
        </Accordion>
    );
}

// --- HOVEDKOMPONENT ---
export function Import() {
    const activeImportDevices = useSseSelector(state => state.importDevices || {});
    const activeImportsMap = useSseSelector(state => state.activeMediaImports || {});
    const activeImports = Object.values(activeImportsMap) as Array<ImportProgressEvent>;

    const [history, setHistory] = useState<Array<DeviceImport>>([]);
    const [historyLoaded, setHistoryLoaded] = useState(false);

    const fetchHistory = async () => {
        try {
            const data = await getHistoricalImports();
            setHistory(data);
            setHistoryLoaded(true);
        } catch (e) {
            toast.error("Kunne ikke hente import-historikk");
        }
    };

    const handleCancel = async (deviceId: string) => {
        try {
            await cancelImportFor(deviceId);
            toast.info("Sender avbryt-signal til enhet...");
        } catch (e) {
            toast.error("Kunne ikke avbryte import");
        }
    };

    return (
        <Box sx={{ p: 3, maxWidth: 900, mx: "auto" }}>
            <Typography variant="h4" gutterBottom>Media Import</Typography>

            <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>Aktive pågående importer</Typography>
            <ActiveImportsList
                activeImports={activeImports}
                activeImportDevices={activeImportDevices}
                onCancel={handleCancel}
            />

            <ImportHistoryList
                history={history}
                historyLoaded={historyLoaded}
                onFetchHistory={fetchHistory}
            />
        </Box>
    );
}