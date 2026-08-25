import { useState } from "react";
import { Box, Typography, Paper, Chip, Avatar, Stack, IconButton, Dialog, DialogTitle, FormControlLabel, Switch, List, DialogContent, ListItemText, ListItem } from "@mui/material";
import CameraAltOutlinedIcon from "@mui/icons-material/CameraAltOutlined";
import SmartphoneOutlinedIcon from '@mui/icons-material/SmartphoneOutlined';
import SdStorageOutlinedIcon from '@mui/icons-material/SdStorageOutlined';
import CableIcon from '@mui/icons-material/Cable';
import BatteryChargingFullIcon from "@mui/icons-material/BatteryChargingFull";
import StorageIcon from "@mui/icons-material/Storage";
import SettingsIcon from "@mui/icons-material/Settings";
import { updateDeviceSettings } from "../../api/requests/camera";
import type { DeviceInfo } from "../../types/types";
import { formatBytes } from "../../utils/format";

export function DeviceCard({ device }: { device: DeviceInfo }) {
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

    const getDeviceDetails = () => {
        switch (device.type) {
            case "BLOCK":
                return { icon: <SdStorageOutlinedIcon fontSize="large" />, label: "Disk", color: "secondary.main" as const, chipColor: "secondary" as const };
            case "MTP":
                return { icon: <SmartphoneOutlinedIcon fontSize="large" />, label: "Mobile", color: "primary.main" as const, chipColor: "primary" as const };
            case "PTP":
                return { icon: <CameraAltOutlinedIcon fontSize="large" />, label: "Camera", color: "info.main" as const, chipColor: "info" as const };
            default:
                return { icon: <CableIcon fontSize="large" />, label: "Device", color: "grey.500" as const, chipColor: "default" as const };
        }
    };

    const details = getDeviceDetails();

    return (
        <>
            <Paper sx={{ p: 3, backgroundColor: "background.paper", height: "100%", boxSizing: "border-box" }}>

                {/* --- 1. LITEN SKJERM (XS) --- Banner-layout --- */}
                <Box sx={{ display: { xs: "flex", md: "none" }, flexDirection: "column", position: "relative" }}>
                    <IconButton
                        onClick={() => setOpen(true)}
                        size="small"
                        sx={{ position: "absolute", top: 0, right: 0 }}
                    >
                        <SettingsIcon />
                    </IconButton>

                    <Stack direction="row" spacing={2} sx={{ alignItems: "flex-start" }}>
                        <Avatar sx={{ bgcolor: details.color, width: 64, height: 64, borderRadius: 2 }}>
                            {details.icon}
                        </Avatar>

                        <Box sx={{ flexGrow: 1, pr: 4, display: "flex", flexDirection: "column" }}>
                            <Typography variant="h6">{device.friendlyName}</Typography>
                            <Typography variant="caption" color="text.secondary">
                                {device.manufacturer} {device.model}
                            </Typography>
                            {!device.attributes.serialNumber && (
                                <Typography variant="caption" color="text.secondary">
                                    S/N: {device.attributes.serialNumber ?? "N/A"}
                                </Typography>
                            )}

                            <Box sx={{ mt: 1, display: "flex", gap: 1, flexWrap: "wrap", alignItems: "center" }}>
                                {device.attributes.batteryLevel && (
                                    <Chip icon={<BatteryChargingFullIcon />} label={`${device.attributes.batteryLevel}%`} variant="outlined" size="small" />
                                )}
                                {device.storage.map((storage, index) => (
                                    <Chip key={index} icon={<StorageIcon />} label={`${formatBytes(storage.freeSpaceBytes)} ledig`} color="primary" variant="filled" size="small" />
                                ))}
                            </Box>
                        </Box>
                    </Stack>
                </Box>


                {/* --- 2. STOR SKJERM (MD+) --- Vertikalt kort med rad for batteri/lagring og innstillinger --- */}
                <Box sx={{ display: { xs: "none", md: "flex" }, flexDirection: "column", height: "100%" }}>
                    <Stack direction="column" spacing={2} sx={{ height: "100%" }}>

                        {/* Stort ikon */}
                        <Avatar sx={{ bgcolor: details.color, width: "100%", height: 160, borderRadius: 2 }}>
                            {details.icon}
                        </Avatar>

                        {/* Stats-linje: Batteri/lagring til venstre, tannhjul helt til høyre på samme linje */}
                        <Stack direction="row" sx={{ alignItems: "center", justifyContent: "space-between" }}>
                            <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap", gap: 1 }}>
                                {device.storage.map((storage, index) => (
                                    <Chip key={index} icon={<StorageIcon />} label={`${formatBytes(storage.freeSpaceBytes)} ledig`} color="primary" variant="filled" size="small" />
                                ))}
                                {device.attributes.batteryLevel && (
                                    <Chip icon={<BatteryChargingFullIcon />} label={`${device.attributes.batteryLevel}%`} variant="outlined" size="small" />
                                )}
                            </Stack>

                            <IconButton onClick={() => setOpen(true)} size="small">
                                <SettingsIcon />
                            </IconButton>
                        </Stack>

                        {/* Navn og info nederst */}
                        <Box>
                            <Typography variant="h5">{device.friendlyName}</Typography>
                            <Typography variant="body2" color="text.secondary">
                                {device.manufacturer} {device.model} • S/N: {device.attributes.serialNumber ?? "N/A"}
                            </Typography>
                        </Box>

                    </Stack>
                </Box>

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
    );
}