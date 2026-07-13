import { Box, Typography, Card, CardContent, Grid, Chip, Stack } from "@mui/material";
import CableIcon from '@mui/icons-material/Cable';
import SdStorageOutlinedIcon from '@mui/icons-material/SdStorageOutlined';
import DvrIcon from '@mui/icons-material/Dvr'; // For SATA/Disk
import SpeedIcon from '@mui/icons-material/Speed'; // For NVMe
import CameraAltOutlinedIcon from '@mui/icons-material/CameraAltOutlined';
import SmartphoneOutlinedIcon from '@mui/icons-material/SmartphoneOutlined';

import { useSseSelector } from "../sse/useSseSelector";
import type { BlockDevice, GPhoto2Device, RemovableDevice } from "../types/types";

export default function Devices() {
    const devices = useSseSelector(state => state.devices);

    const getPath = (d: RemovableDevice) => {
        if (d.type === "BLOCK") {
            // TypeScript vet nå at 'd' har mountPoint/devicePath
            return (d as BlockDevice).mountPoint || "Not mounted";
        }
        if (d.type === "MTP" || d.type === "PTP") {
            return (d as GPhoto2Device).port;
        }
        return "Unknown path";
    };

    const getDeviceDetails = (d: RemovableDevice) => {
        switch (d.type) {
            case "BLOCK":
                return { icon: <SdStorageOutlinedIcon />, label: "Disk", color: "secondary" as const };
            case "MTP":
                return { icon: <SmartphoneOutlinedIcon />, label: "Mobile", color: "primary" as const };
            case "PTP":
                return { icon: <CameraAltOutlinedIcon />, label: "Camera", color: "info" as const };
            default:
                return { icon: <CableIcon />, label: "Device", color: "default" as const };
        }
    };

    return (
        <Box sx={{ p: 3 }}>
            <Typography variant="h4" gutterBottom>Connected Devices</Typography>
            <Grid container spacing={2}>
                {devices.map(d => {
                    const details = getDeviceDetails(d);
                    return (
                        <Grid key={d.name}>
                            <Card variant="outlined" sx={{ height: '100%' }}>
                                <CardContent>
                                    <Stack direction="row" sx={{
                                        alignItems: "center"
                                    }} spacing={2}>
                                        {details.icon}
                                        <Box>
                                            <Typography variant="h6">{d.model || "Unknown Device"}</Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                {getPath(d)} • {d.id || "No SN"}
                                            </Typography>
                                        </Box>
                                    </Stack>
                                    <Box sx={{ mt: 2 }}>
                                        <Chip
                                            label={d.type === "BLOCK" && !(d as BlockDevice).mountPoint ? "Unmounted" : "Ready"}
                                            color={d.type === "BLOCK" && !(d as BlockDevice).mountPoint ? "default" : "success"}
                                            size="small"
                                        />
                                        <Chip
                                            label={details.label}
                                            color={details.color}
                                            size="small"
                                            sx={{ ml: 1 }}
                                        />
                                    </Box>
                                </CardContent>
                            </Card>
                        </Grid>
                    );
                })}
            </Grid>
        </Box>
    );
}