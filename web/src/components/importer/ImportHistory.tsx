import { useEffect, useState } from "react";
import {
    Box, Typography, Paper, LinearProgress, Stack,
    Accordion, AccordionSummary, AccordionDetails, Collapse,
    IconButton, Chip, TextField
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import HistoryIcon from '@mui/icons-material/History';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import type { DeviceImport, ImportProgressEvent } from "../../types/types";


export function ImportHistoryList({ history, historyLoaded, onFetchHistory }: {
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