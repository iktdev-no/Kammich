import { useState } from "react";
import { Box, Typography, Paper, ListItemIcon, ListItemText, Menu, MenuItem } from "@mui/material";
import type { DeviceSettingsDto, WFile } from "../../types/types";
import FolderIcon from '@mui/icons-material/Folder';
import PhotoIcon from '@mui/icons-material/Photo';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined';


import DoNotDisturbAltOutlinedIcon from '@mui/icons-material/DoNotDisturbAltOutlined';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';

export default function DeviceFileView({ files, onNavigate, settings, onSettingsChange }: {
    files: Array<WFile>, onNavigate: (path: string) => void, settings: DeviceSettingsDto | null,
    onSettingsChange: (newSettings: Partial<DeviceSettingsDto>) => void
}) {
    console.log(files);

    const [contextMenu, setContextMenu] = useState<{ mouseX: number; mouseY: number; file: WFile } | null>(null);

    const handleContextMenu = (event: React.MouseEvent, file: WFile) => {
        event.preventDefault();
        setContextMenu(
            contextMenu === null
                ? { mouseX: event.clientX + 2, mouseY: event.clientY - 6, file }
                : null,
        );
    };

    const handleClose = () => setContextMenu(null);

    if (files.length === 0) {
        return (
            <Box sx={{
                display: "flex", flexDirection: "column", alignItems: "center",
                justifyContent: "center", py: 10, color: "text.secondary"
            }}>
                <InboxOutlinedIcon sx={{ fontSize: 80, mb: 2, opacity: 0.5 }} />
                <Typography variant="h6">Denne mappen er tom</Typography>
            </Box>
        );
    }

    const handleMenuAction = async (action: 'Include' | 'Exclude') => {
        if (!contextMenu) return;

        const path = contextMenu.file.path;
        const currentIncludes = settings?.includeFolders || [];
        const currentExcludes = settings?.excludeFolders || [];

        // Logikk for å oppdatere listene
        let newIncludes = [...currentIncludes];
        let newExcludes = [...currentExcludes];

        if (action === 'Include') {
            newExcludes = newExcludes.filter(p => p !== path);
            if (!newIncludes.includes(path)) newIncludes.push(path);
        } else {
            newIncludes = newIncludes.filter(p => p !== path);
            if (!newExcludes.includes(path)) newExcludes.push(path);
        }

        // Send oppdatering til Camera-komponenten
        onSettingsChange({ includeFolders: newIncludes, excludeFolders: newExcludes });
        handleClose();
    };

    return (
        <>
            <Box sx={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(140px, 1fr))", gap: 2 }}>
                {files.map((file) => {
                    const isFolder = file.type === "DIRECTORY";
                    const isImage = /\.(jpg|jpeg|png|heic|cr2|nef)$/i.test(file.name);

                    return (
                        <Paper
                            key={file.id}
                            elevation={0}
                            onClick={() => isFolder && onNavigate(file.path)}
                            onContextMenu={(e) => handleContextMenu(e, file)} // 3. Høyreklikk-handler
                            sx={{
                                p: 1,
                                cursor: "pointer", // LAGT TIL: cursor pointer
                                display: "flex",
                                flexDirection: "column",
                                alignItems: "center",
                                backgroundColor: "rgba(255,255,255,0.03)",
                                borderRadius: 3,
                                transition: "transform 0.2s",
                                "&:hover": { backgroundColor: "rgba(255,255,255,0.08)", transform: "scale(1.02)" }
                            }}
                        >
                            {/* ... resten av innholdet ditt ... */}
                            <Box sx={{ height: 100, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                {isFolder ? <FolderIcon sx={{ fontSize: 60, color: "primary.main" }} /> :
                                    isImage ? <PhotoIcon sx={{ fontSize: 60, color: "secondary.main" }} /> :
                                        <InsertDriveFileOutlinedIcon sx={{ fontSize: 60, color: "text.secondary" }} />}
                            </Box>
                            <Typography variant="caption" noWrap sx={{ width: "100%", textAlign: "center", mt: 1, px: 1 }}>
                                {file.name}
                            </Typography>
                        </Paper>
                    );
                })}
            </Box>

            {/* 4. Selve Kontekstmenyen */}
            <Menu
                open={contextMenu !== null}
                onClose={handleClose}
                anchorReference="anchorPosition"
                anchorPosition={
                    contextMenu !== null ? { top: contextMenu.mouseY, left: contextMenu.mouseX } : undefined
                }
            >
                <MenuItem onClick={() => { handleMenuAction('Include') }}>
                    <ListItemIcon><FileUploadOutlinedIcon fontSize="small" /></ListItemIcon>
                    <ListItemText>Import</ListItemText>
                </MenuItem>
                <MenuItem onClick={() => { handleMenuAction('Exclude') }}>
                    <ListItemIcon><DoNotDisturbAltOutlinedIcon fontSize="small" /></ListItemIcon>
                    <ListItemText>Exclude</ListItemText>
                </MenuItem>
            </Menu>
        </>
    )
}