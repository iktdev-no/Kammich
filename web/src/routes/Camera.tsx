import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Box, Typography, Paper, Divider, Chip, Avatar, Stack, Button, CircularProgress, ListItemIcon, ListItemText, Menu, MenuItem, IconButton, Dialog, DialogTitle, FormControlLabel, Switch, List, DialogContent, ListItem } from "@mui/material";
import CameraAltOutlinedIcon from "@mui/icons-material/CameraAltOutlined";
import BatteryChargingFullIcon from "@mui/icons-material/BatteryChargingFull";
import StorageIcon from "@mui/icons-material/Storage";
import { getDeviceInfo, getFiles, updateDeviceSettings } from "../api/camera";
import type { DeviceInfo, DeviceSettingsDto, WFile } from "../types/types";
import FolderIcon from '@mui/icons-material/Folder';
import PhotoIcon from '@mui/icons-material/Photo';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined';
import { BreadcrumbPath } from "../components/BreadcrumbPath";
import { useSseSelector } from "../sse/useSseSelector";
import UsbOffIcon from '@mui/icons-material/UsbOff';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import SettingsIcon from "@mui/icons-material/Settings";
import { toast } from "react-toastify";


import DoNotDisturbAltOutlinedIcon from '@mui/icons-material/DoNotDisturbAltOutlined';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import { formatBytes } from "../utils/format";

type CameraState =
    | { type: 'DISCONNECTED' }
    | { type: 'CONNECTING' }
    | { type: 'WAITING_FOR_PERMISSION'; device: DeviceInfo }
    | { type: 'READY'; device: DeviceInfo; files: WFile[] };

interface ActiveConnection {
    sn: string
    connected: boolean
}


function useStableConnection(sn: string | undefined): ActiveConnection | undefined {
    const devices = useSseSelector(state => state.devices);
    const isPresent = devices.some(d => d.id === sn);
    const [connection, setConnection] = useState<ActiveConnection | undefined>(
        isPresent ? { sn: sn!, connected: true } : undefined
    );

    useEffect(() => {
        if (isPresent) {
            setConnection({ sn: sn!, connected: true });
        } else {
            const timer = setTimeout(() => setConnection(undefined), 1500);
            return () => clearTimeout(timer);
        }
    }, [isPresent, sn]);

    return connection;
}

function useCameraEngine(sn: string | undefined, currentPath: string): CameraState {
    const connection = useStableConnection(sn);

    const [info, setInfo] = useState<DeviceInfo | null>(null);
    const [files, setFiles] = useState<WFile[]>([]);
    const [isReconnecting, setIsReconnecting] = useState(false);

    useEffect(() => {
        // Hvis SN endres, nullstill info med en gang for å trigge ny fetch
        setInfo(null);
        setFiles([]);
    }, [sn]);

    // Hent data - trigger på tilkobling eller behov for tillatelse
    useEffect(() => {
        if (!connection) {
            // Vi nuller IKKE ut her, vi beholder info til Connection faktisk dør
            setFiles([])
            if (info) {
                setInfo(prev => prev ? { ...prev, storage: [] } : null);
            }
            return;
        }

        const needsFetch = !info || (info.storage.length === 0);

        if (needsFetch && !isReconnecting) {
            setIsReconnecting(true);
            getDeviceInfo(sn!)
                .then(setInfo)
                .catch(() => setInfo(null))
                .finally(() => setIsReconnecting(false));
        }
    }, [connection, sn]);

    // Fil-henting
    useEffect(() => {
        if (info && info.storage.length > 0) {
            getFiles(sn!, currentPath)
                .then(setFiles)
                .catch(console.error);
        }
    }, [currentPath, info, sn]);

    // Utled State
    if (!connection) return { type: 'DISCONNECTED' };
    if (isReconnecting || !info) return { type: 'CONNECTING' };
    if (info.storage.length === 0) return { type: 'WAITING_FOR_PERMISSION', device: info };
    return { type: 'READY', device: info, files };
}

export default function Camera() {
    const [settings, setSettings] = useState<DeviceSettingsDto | null>(null);
    const { sn, "*": splat } = useParams<{ sn: string, "*": string }>();
    const currentPath = splat ? `/${splat}` : "/";
    const navigate = useNavigate();

    const handleSettingsUpdate = async (newSettings: Partial<DeviceSettingsDto>) => {
        try {
            // Oppdater backend
            const updatedDevice = await updateDeviceSettings(sn!, newSettings);
            // Oppdater lokal state - dette trigger en re-render av hele Camera
            setSettings(updatedDevice.deviceSettings);
        } catch (e) {
            toast.error("Kunne ikke oppdatere innstillinger");
        }
    };

    const state = useCameraEngine(sn, currentPath);
    useEffect(() => {
        if (state.type === 'READY') {
            setSettings(state.device.deviceSettings);
        }
    }, [state]);
    switch (state.type) {
        case 'DISCONNECTED':
            return <NoDeviceConnected sn={sn!} />;

        case 'CONNECTING':
            return <StatusSpinner message="Kobler til enhet..." timeout={30000} />;

        case 'WAITING_FOR_PERMISSION':
            return (
                <Box sx={{ p: 3 }}>
                    <CameraBanner device={state.device} />
                    <StatusSpinner message="Vennligst trykk 'Tillat' på telefonen." />
                </Box>
            );

        case 'READY':
            return (
                <Box sx={{ p: 3 }}>
                    <CameraBanner device={state.device} />
                    <BreadcrumbPath path={currentPath} onNavigate={(p) => navigate(`/camera/${sn}${p}`)} />
                    <FileView files={state.files} onNavigate={(p) => navigate(`/camera/${sn}${p}`)}
                        settings={settings} onSettingsChange={handleSettingsUpdate} />
                </Box>
            );
    }

}

function StatusSpinner({ message, timeout }: { message: string, timeout?: number }) {
    const [show, setShow] = useState(true);

    useEffect(() => {
        if (timeout) {
            const t = setTimeout(() => setShow(false), timeout);
            return () => clearTimeout(t);
        }
    }, [timeout]);

    if (!show && timeout) return <NoDeviceConnected sn="ukjent" />; // Fallback hvis timeout går ut

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 10, gap: 2 }}>
            <CircularProgress size={60} />
            <Typography variant="h5">{message}</Typography>
        </Box>
    );
}

function CameraBanner({ device }: { device: DeviceInfo }) {
    const [open, setOpen] = useState(false);
    const [settings, setSettings] = useState(device.deviceSettings || { autoImport: true, includeFolders: [], excludeFolders: [] });

    const handleToggleAutoImport = async () => {
        const newValue = !settings.autoImport;
        setSettings(prev => ({ ...prev, autoImport: newValue }));
        try {
            await updateDeviceSettings(device.id, { autoImport: newValue });
        } catch (e) {
            setSettings(prev => ({ ...prev, autoImport: !newValue })); // Rollback
        }
    };

    return (
        <>
            <Paper sx={{ p: 3, mb: 3, backgroundColor: "background.paper" }}>
                <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
                    <Avatar sx={{ bgcolor: "primary.main", width: 56, height: 56 }}>
                        <CameraAltOutlinedIcon fontSize="large" />
                    </Avatar>

                    <Box sx={{ flexGrow: 1 }}>
                        <Typography variant="h5">{device.friendlyName}</Typography>
                        <Typography variant="body2" color="text.secondary">
                            {device.manufacturer} {device.model} • S/N: {device.attributes.serialNumber ?? "N/A"}
                        </Typography>
                    </Box>

                    {/* Innstillinger-knapp */}
                    <IconButton onClick={() => setOpen(true)}><SettingsIcon /></IconButton>

                    <Stack direction="row" spacing={1}>
                        {device.attributes.batteryLevel && (
                            <Chip icon={<BatteryChargingFullIcon />} label={`${device.attributes.batteryLevel}%`} variant="outlined" />
                        )}
                        {device.storage.map((storage, index) => (
                            <Chip key={index} icon={<StorageIcon />} label={`${formatBytes(storage.freeSpaceBytes)} ledig`} color="primary" variant="filled" />
                        ))}
                    </Stack>
                </Stack>
            </Paper>

            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="xs">
                <DialogTitle>Enhetsinnstillinger</DialogTitle>
                <DialogContent>
                    <FormControlLabel
                        control={<Switch checked={settings.autoImport ?? true} onChange={handleToggleAutoImport} />}
                        label="Auto-import ved tilkobling"
                    />
                    <Typography variant="subtitle2" sx={{ mt: 2 }}>Inkluderte mapper:</Typography>
                    <List dense>
                        {settings.includeFolders?.map(f => (
                            <ListItem key={f}><ListItemText primary={f} /></ListItem>
                        ))}
                    </List>
                </DialogContent>
            </Dialog>
        </>
    )
}


function FileView({ files, onNavigate, settings, onSettingsChange }: {
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

function NoDeviceConnected({ sn }: { sn: string }) {
    const navigate = useNavigate();
    return (
        <Box sx={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            justifyContent: 'center', height: '80vh', textAlign: 'center', p: 3
        }}>
            <UsbOffIcon sx={{ fontSize: 100, color: 'text.secondary', mb: 2, opacity: 0.5 }} />
            <Typography variant="h4" gutterBottom>Enhet frakoblet</Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Enheten med serienummer <strong>{sn}</strong> ble ikke funnet.
                Sjekk kabelen eller om enheten fortsatt er tilkoblet.
            </Typography>
            <Button
                variant="contained"
                onClick={() => navigate("/")}
                startIcon={<ArrowUpwardIcon style={{ transform: 'rotate(-90deg)' }} />}
            >
                Gå til oversikt
            </Button>
        </Box>
    );
}
