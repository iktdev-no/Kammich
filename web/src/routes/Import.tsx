import { useEffect, useState } from "react";
import {
    Box, Typography, Paper, LinearProgress, Stack,
    Collapse, IconButton, Chip
} from "@mui/material";
import CancelIcon from '@mui/icons-material/Cancel';
import DownloadDoneIcon from '@mui/icons-material/DownloadDone';

import type { DeviceImport, DeviceImportSummary, ImportProgressEvent, FileImportState } from "../types/types";
import { toast } from "react-toastify";
import { cancelImportFor, getHistoricalImports } from "../api/importer";
import { ImportHistoryList } from "../components/importer/ImportHistory";
import { useSseSelector } from "../sse/useSseSelector";
import ImportFileStream from "../components/importer/ImportFileStream";

// --- DELKOMPONENT: Enhetskort ---
function DeviceImportCard({ deviceId, summary, progressEvent, onCancel }: {
    deviceId: string;
    summary: DeviceImportSummary;
    progressEvent?: ImportProgressEvent;
    onCancel: (deviceId: string) => void;
}) {
    const completedFiles = progressEvent?.completedFiles ?? 0;
    const totalFiles = progressEvent?.totalFiles ?? 0;
    const progress = totalFiles > 0 ? (completedFiles / totalFiles) * 100 : 0;
    const currentFile = progressEvent?.currentFile || null;
    const files = progressEvent?.files || [];
    const hasProgress = Boolean(progressEvent);
    const isCompleted = summary.state === "Completed";

    return (
        <Paper variant="outlined" sx={{ p: 2.5, position: "relative" }}>
            {/* Header */}
            <Stack sx={{ flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", mb: 1.5 }}>
                <Box sx={{ minWidth: 0, flex: 1, mr: 2 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: "bold" }}>
                        {deviceId}
                    </Typography>
                </Box>
                <Stack sx={{ flexDirection: "row", gap: 1.5, alignItems: "center", flexShrink: 0 }}>
                    <Chip
                        label={hasProgress ? `${completedFiles} av ${totalFiles} filer • ${summary.state}` : `Forbereder... • ${summary.state}`}
                        size="small"
                        color={isCompleted ? "success" : "primary"}
                    />
                    {!isCompleted && (
                        <IconButton
                            size="small"
                            color="error"
                            onClick={() => onCancel(deviceId)}
                            title="Avbryt import for enhet"
                            sx={{ border: "1px solid", borderColor: "error.light" }}
                        >
                            <CancelIcon />
                        </IconButton>
                    )}
                </Stack>
            </Stack>



            {/* Progresjonsbar */}
            <Box sx={{ mb: 2 }}>
                <LinearProgress
                    variant={hasProgress ? "determinate" : "indeterminate"}
                    value={progress}
                    sx={{ height: 8, borderRadius: 4 }}
                    color={isCompleted ? "success" : "primary"}
                />
            </Box>

            {/* Filliste */}
            <ImportFileStream files={files} currentFile={currentFile} />
        </Paper>
    );
}

// --- HOVEDKOMPONENT ---
export function ActiveImportsList({ activeDevices, activeImportsMap, onCancel }: {
    activeDevices: Record<string, DeviceImportSummary>;
    activeImportsMap: Record<string, ImportProgressEvent>;
    onCancel: (deviceId: string) => void;
}) {
    const [recentCompleted, setRecentCompleted] = useState<Array<DeviceImportSummary & { totalFiles?: number }>>([]);

    const deviceEntries = Object.entries(activeDevices);

    useEffect(() => {
        deviceEntries.forEach(([deviceId, summary]) => {
            if (!summary) return;
            const isFinished = summary.state === "Completed";
            if (isFinished) {
                const progressEvent = activeImportsMap[deviceId];
                setRecentCompleted(prev => {
                    // Legger til i listen hvis den ikke finnes fra før, uten automatisk sletting (fjernet setTimeout)
                    if (!prev.some(item => item?.deviceId === deviceId)) {
                        return [{ ...summary, totalFiles: progressEvent?.totalFiles || 0 }, ...prev].slice(0, 5);
                    }
                    return prev;
                });
            }
        });
    }, [activeDevices, activeImportsMap]);

    if (deviceEntries.length === 0 && recentCompleted.length === 0) {
        return (
            <Paper sx={{ p: 3, textAlign: "center", color: "text.secondary", mb: 3 }}>
                Ingen aktive importer for øyeblikket.
            </Paper>
        );
    }

    return (
        <Stack sx={{ gap: 2, mb: 3 }}>
            {deviceEntries.map(([deviceId, summary]) => {
                if (!summary) return null;
                return (
                    <DeviceImportCard
                        key={deviceId}
                        deviceId={deviceId}
                        summary={summary}
                        progressEvent={activeImportsMap[deviceId]}
                        onCancel={onCancel}
                    />
                );
            })}
        </Stack>
    );
}

// --- HOVEDKOMPONENT ---
export function Import() {
    const activeImportDevices = useSseSelector(state => state.importDevices || {}) as Record<string, DeviceImportSummary>;
    const activeImportsMap = useSseSelector(state => state.activeMediaImports || {}) as Record<string, ImportProgressEvent>;

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
                activeDevices={activeImportDevices}
                activeImportsMap={activeImportsMap}
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