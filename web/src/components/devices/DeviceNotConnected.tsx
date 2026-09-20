import { Box, Typography, Button } from "@mui/material";
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import UsbOffIcon from '@mui/icons-material/UsbOff';
import { Trans, useTranslation } from "react-i18next";


interface NoDeviceConnectedProps {
    sn: string,
    onGoBack: () => void;
}

export default function DeviceNotConnected({ sn, onGoBack }: NoDeviceConnectedProps) {
    const { t } = useTranslation();
    return (
        <Box sx={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            justifyContent: 'center', height: '80vh', textAlign: 'center', p: 3
        }}>
            <UsbOffIcon sx={{ fontSize: 100, color: 'text.secondary', mb: 2, opacity: 0.5 }} />
            <Typography variant="h4" gutterBottom>{t('devices.device_disconnected')}</Typography>
            <Typography
                variant="body1"
                color="text.secondary"
                sx={{ mb: 4 }}
            >
                <Trans
                    i18nKey="devices.unavailable"
                    values={{
                        serialNumber: sn,
                    }}
                    components={{
                        strong: <strong />,
                    }}
                />

                <br />

                {t("devices.checkConnection")}
            </Typography>
            <Button
                variant="contained"
                onClick={onGoBack}
                startIcon={<ArrowUpwardIcon style={{ transform: 'rotate(-90deg)' }} />}
            >
                {t('devices.go_to_overview')}
            </Button>
        </Box>
    );
}
