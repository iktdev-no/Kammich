import type { DeviceOwnershipSummary } from "../../types/types"
import {
    Box,
    Typography,
    Button,
    Card,
    CardContent,
    Chip,
    Divider
} from "@mui/material"
import {
    Storage as StorageIcon,
    Smartphone as SmartphoneIcon,
    PhotoCamera as CameraIcon,
    Devices as DevicesIcon,
    CheckCircle as CheckCircleIcon,
    Lock as LockIcon
} from "@mui/icons-material"

export interface DeviceClaimCardProps {
    device: DeviceOwnershipSummary;
    onClaimDevice: (serialNumber: string) => void;
}

export function DeviceClaimCard({ device, onClaimDevice }: DeviceClaimCardProps) {
    const isClaimed = Boolean(device.claimedBy)

    const getDeviceIcon = (type: string) => {
        switch (type) {
            case "Phone": return <SmartphoneIcon fontSize="small" />
            case "Camera": return <CameraIcon fontSize="small" />
            case "PhysicalStorageDevice": return <StorageIcon fontSize="small" />
            default: return <DevicesIcon fontSize="small" />
        }
    }

    return (
        <Card
            variant="outlined"
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 3,
                transition: 'border-color 0.2s',
                '&:hover': { borderColor: 'primary.main' }
            }}
        >
            <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2 }}>
                    <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
                        <Box sx={{ p: 1, borderRadius: 2, bgcolor: 'action.selected', display: 'flex' }}>
                            {getDeviceIcon(device.deviceType)}
                        </Box>
                        <Box>
                            <Typography variant="subtitle1" sx={{ fontWeight: 600, lineHeight: 1.2 }}>
                                {device.name}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                {device.manufacturer} {device.model}
                            </Typography>
                        </Box>
                    </Box>
                    <Chip label={device.deviceType} size="small" variant="outlined" sx={{ fontSize: '0.7rem' }} />
                </Box>

                <Typography variant="caption" sx={{ fontFamily: 'monospace', color: 'text.secondary', display: 'block', mt: 2 }}>
                    SN: {device.deviceId}
                </Typography>
            </CardContent>

            <Box>
                <Divider />
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    {isClaimed ? (
                        <Chip
                            icon={<LockIcon fontSize="small" />}
                            label="Eies av bruker"
                            color="warning"
                            size="small"
                            variant="outlined"
                        />
                    ) : (
                        <Chip
                            icon={<CheckCircleIcon fontSize="small" />}
                            label="Tilgjengelig"
                            color="success"
                            size="small"
                            variant="outlined"
                        />
                    )}

                    {device.claimable && (
                        <Button
                            variant="contained"
                            size="small"
                            onClick={() => onClaimDevice(device.deviceId)}
                            sx={{ textTransform: 'none', borderRadius: 2, boxShadow: 'none' }}
                        >
                            Ta eierskap
                        </Button>
                    )}
                </Box>
            </Box>
        </Card>
    )
}