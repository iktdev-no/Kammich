import { useEffect, useState } from "react";
import {
    Box,
    Container,
    Typography,
    Card,
    CardContent,
    Button,
    Stack,
    Alert,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    DialogActions,
    CircularProgress,
} from "@mui/material";
import PowerSettingsNewIcon from "@mui/icons-material/PowerSettingsNew";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import type { PowerPermissionsDto } from "../types/types";
import { executePowerOff, executeReboot, getPowerPermissions } from "../api/requests/system";
import { useTranslation } from "react-i18next";

export function System() {
    const { t } = useTranslation()
    const [permissions, setPermissions] = useState<PowerPermissionsDto | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [actionLoading, setActionLoading] = useState<boolean>(false);
    const [feedback, setFeedback] = useState<{ message: string; severity: "success" | "error" } | null>(null);

    // Dialog-tilstand for bekreftelse
    const [confirmAction, setConfirmAction] = useState<"poweroff" | "reboot" | null>(null);

    const fetchPermissions = () => {
        setLoading(true);
        getPowerPermissions()
            .then(res => setPermissions(res))
            .catch(err => {
                console.error("Klarte ikke å hente strømtillatelser", err);
                setFeedback({ message: t('common.system_permission_error1'), severity: "error" });
            })
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        fetchPermissions();
    }, []);

    const handleExecute = async () => {
        if (!confirmAction) return;

        setActionLoading(true);
        setFeedback(null);

        try {
            const res = confirmAction === "poweroff"
                ? await executePowerOff()
                : await executeReboot();

            setFeedback({ message: res.message, severity: res.success ? "success" : "error" });
        } catch (err: any) {
            console.error("Feil ved utførelse av strømkommando", err);
            setFeedback({ message: err.message || t('common.unexpected_error'), severity: "error" });
        } finally {
            setActionLoading(false);
            setConfirmAction(null);
        }
    };

    return (
        <Container maxWidth="md" sx={{ py: { xs: 2, md: 4 } }}>
            <Box sx={{ mb: 4 }}>
                <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
                    {t('system.title')}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    {t('system.body')}
                </Typography>
            </Box>

            {feedback && (
                <Alert severity={feedback.severity} sx={{ mb: 3 }} onClose={() => setFeedback(null)}>
                    {feedback.message}
                </Alert>
            )}

            {loading ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
                    <CircularProgress />
                </Box>
            ) : (
                <Card
                    sx={{
                        backgroundColor: "background.paper",
                        borderRadius: 3,
                        border: "1px solid rgba(255,255,255,0.06)"
                    }}
                >
                    <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                        <Stack spacing={3}>
                            <Box>
                                <Typography variant="h6" sx={{ mb: 0.5, fontWeight: 600 }}>
                                    {t('system.actions.title')}
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    {t('system.actions.body')}
                                </Typography>
                            </Box>

                            <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                                <Button
                                    variant="outlined"
                                    color="warning"
                                    startIcon={<RestartAltIcon />}
                                    onClick={() => setConfirmAction("reboot")}
                                    disabled={!permissions?.canReboot || actionLoading}
                                    fullWidth
                                    sx={{ py: 1.5 }}
                                >
                                    {t('system.actions.reboot')}
                                </Button>

                                <Button
                                    variant="contained"
                                    color="error"
                                    startIcon={<PowerSettingsNewIcon />}
                                    onClick={() => setConfirmAction("poweroff")}
                                    disabled={!permissions?.canPowerOff || actionLoading}
                                    fullWidth
                                    sx={{ py: 1.5 }}
                                >
                                    {t('system.actions.shutdown')}
                                </Button>
                            </Stack>

                            {(!permissions?.canReboot && !permissions?.canPowerOff) && (
                                <Typography variant="caption" color="text.secondary" sx={{ fontStyle: "italic", textAlign: "center" }}>
                                    {t('system.actions.not_permitted')}
                                </Typography>
                            )}
                        </Stack>
                    </CardContent>
                </Card>
            )}

            {/* Bekreftelsesdialog */}
            <Dialog open={confirmAction !== null} onClose={() => setConfirmAction(null)}>
                <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                    <WarningAmberIcon color="warning" />
                    {confirmAction === "poweroff" ? t('system.actions.shutdown_confirm') : t('system.actions.reboot_confirm')}
                </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        {confirmAction === "poweroff" ? t('system.actions.confirm.shutdown') : t('system.actions.confirm.reboot')}
                    </DialogContentText>
                </DialogContent>
                <DialogActions sx={{ p: 2, pt: 0 }}>
                    <Button onClick={() => setConfirmAction(null)} color="inherit" disabled={actionLoading}>
                        {t('common.cancel')}
                    </Button>
                    <Button
                        onClick={handleExecute}
                        variant="contained"
                        color={confirmAction === "poweroff" ? "error" : "warning"}
                        disabled={actionLoading}
                        autoFocus
                    >
                        {actionLoading ? t('system.actions.confirm.executes') : t('system.actions.confirm.title')}
                    </Button>
                </DialogActions>
            </Dialog>
        </Container>
    );
}