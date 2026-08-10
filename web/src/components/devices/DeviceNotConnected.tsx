import { Box, Typography, Button } from "@mui/material";
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import UsbOffIcon from '@mui/icons-material/UsbOff';


interface NoDeviceConnectedProps {
    sn: string,
    onGoBack: () => void;
}

export default function DeviceNotConnected({ sn, onGoBack }: NoDeviceConnectedProps) {
    return (
        <Box sx={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            justifyContent: 'center', height: '80vh', textAlign: 'center', p: 3
        }}>
            <UsbOffIcon sx={{ fontSize: 100, color: 'text.secondary', mb: 2, opacity: 0.5 }} />
            <Typography variant="h4" gutterBottom>Enheten er frakoblet</Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                Enheten med serienummer <strong>{sn}</strong> ble frakoblet eller utilgjengelig. <br />
                Sjekk kabelen eller om enheten fortsatt er tilkoblet.
            </Typography>
            <Button
                variant="contained"
                onClick={onGoBack}
                startIcon={<ArrowUpwardIcon style={{ transform: 'rotate(-90deg)' }} />}
            >
                Gå til oversikt
            </Button>
        </Box>
    );
}
