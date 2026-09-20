import {
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    IconButton,
    InputAdornment,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography,
    Box,
    useTheme,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import CloseIcon from "@mui/icons-material/Close";

import type {
    WifiSecurityType,
    WifiTetherAP,
} from "../../types/types";

import { wifiApi } from "../../api/requests/networking/wifi";

interface AccessPointSettingsDialogProps {
    open: boolean;
    onClose: () => void;
}

export default function AccessPointSettingsDialog({
    open,
    onClose,
}: AccessPointSettingsDialogProps) {
    const theme = useTheme();
    const { t } = useTranslation();

    const [settings, setSettings] =
        useState<WifiTetherAP | null>(null);

    const [loading, setLoading] =
        useState<boolean>(false);

    const [saving, setSaving] =
        useState<boolean>(false);

    const [showPassword, setShowPassword] =
        useState<boolean>(false);

    useEffect(() => {
        if (!open) {
            return;
        }

        setLoading(true);

        wifiApi
            .getAccessPoint()
            .then(settings =>
                setSettings(settings ?? null),
            )
            .finally(() => setLoading(false));
    }, [open]);

    const update = <K extends keyof WifiTetherAP>(
        key: K,
        value: WifiTetherAP[K],
    ): void => {
        setSettings(prev =>
            prev
                ? {
                    ...prev,
                    [key]: value,
                }
                : prev,
        );
    };

    const handleSave = async (): Promise<void> => {
        if (!settings) {
            return;
        }

        setSaving(true);

        try {
            await wifiApi.setAccessPoint(settings);
            onClose();
        } finally {
            setSaving(false);
        }
    };

    const securityLabel = (
        security: WifiSecurityType,
    ): string => {
        switch (security) {
            case "NONE":
                return t(
                    "network.tether_settings.security_none",
                );

            case "WPA2":
                return t(
                    "network.tether_settings.security_wpa2",
                );

            case "WPA3":
                return t(
                    "network.tether_settings.security_wpa3",
                );
        }
    };

    return (
        <Dialog
            open={open}
            onClose={saving ? undefined : onClose}
            fullScreen
        >
            <IconButton
                onClick={onClose}
                disabled={saving}
                aria-label={t("common.cancel")}
                sx={{
                    position: "fixed",
                    top: 20,
                    right: 20,
                    color: "text.primary",
                    bgcolor: "action.hover",
                    "&:hover": {
                        bgcolor: "action.selected",
                    },
                    zIndex: 10,
                }}
            >
                <CloseIcon fontSize="large" />
            </IconButton>

            <DialogContent
                sx={{
                    p: { xs: 2, sm: 4 },
                    overflowY: "auto",
                    bgcolor: theme.palette.background.default,
                }}
            >
                <Box
                    sx={{
                        width: "100%",
                        maxWidth: 900,
                        mx: "auto",
                    }}
                >
                    <DialogTitle
                        sx={{
                            px: 0,
                            pr: 7,
                            fontWeight: 600,
                        }}
                    >
                        {t("network.tether_settings.title")}
                    </DialogTitle>

                    {loading ? (
                        <Box
                            sx={{
                                display: "flex",
                                justifyContent: "center",
                                py: 5,
                            }}
                        >
                            <CircularProgress />
                        </Box>
                    ) : !settings ? (
                        <Typography
                            color="text.secondary"
                            sx={{ py: 2 }}
                        >
                            {t("common.unexpected_error")}
                        </Typography>
                    ) : (
                        <Stack
                            sx={{
                                gap: 2.5,
                                pt: 1,
                            }}
                        >
                            <TextField
                                label={t(
                                    "network.tether_settings.ssid",
                                )}
                                value={settings.ssid}
                                onChange={event =>
                                    update(
                                        "ssid",
                                        event.target.value,
                                    )
                                }
                                fullWidth
                                autoFocus
                            />

                            <FormControl fullWidth>
                                <InputLabel>
                                    {t(
                                        "network.tether_settings.security",
                                    )}
                                </InputLabel>

                                <Select
                                    value={settings.security}
                                    label={t(
                                        "network.tether_settings.security",
                                    )}
                                    onChange={event =>
                                        update(
                                            "security",
                                            event.target
                                                .value as WifiSecurityType,
                                        )
                                    }
                                >
                                    <MenuItem value="NONE">
                                        {securityLabel("NONE")}
                                    </MenuItem>

                                    <MenuItem value="WPA2">
                                        {securityLabel("WPA2")}
                                    </MenuItem>

                                    <MenuItem value="WPA3">
                                        {securityLabel("WPA3")}
                                    </MenuItem>
                                </Select>
                            </FormControl>

                            <TextField
                                label={t(
                                    "network.tether_settings.password",
                                )}
                                value={settings.password}
                                onChange={event =>
                                    update(
                                        "password",
                                        event.target.value,
                                    )
                                }
                                type={
                                    showPassword
                                        ? "text"
                                        : "password"
                                }
                                fullWidth
                                disabled={
                                    settings.security === "NONE"
                                }
                                helperText={
                                    settings.security === "NONE"
                                        ? undefined
                                        : t(
                                            "network.tether_settings.password_hint",
                                        )
                                }
                                slotProps={{
                                    input: {
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <IconButton
                                                    onClick={() =>
                                                        setShowPassword(
                                                            previous =>
                                                                !previous,
                                                        )
                                                    }
                                                    edge="end"
                                                    disabled={
                                                        settings.security ===
                                                        "NONE"
                                                    }
                                                >
                                                    {showPassword ? (
                                                        <VisibilityOffIcon />
                                                    ) : (
                                                        <VisibilityIcon />
                                                    )}
                                                </IconButton>
                                            </InputAdornment>
                                        ),
                                    },
                                }}
                            />
                        </Stack>
                    )}

                    {/* Extra scroll space */}
                    <Box
                        sx={{
                            height: "50vh",
                            minHeight: "50vh",
                            flexShrink: 0,
                        }}
                    />
                </Box>
            </DialogContent>

            <DialogActions
                sx={{
                    px: { xs: 2, sm: 4 },
                    py: 2,
                    justifyContent: "flex-end",
                }}
            >
                <Box
                    sx={{
                        width: "100%",
                        maxWidth: 900,
                        mx: "auto",
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: 1,
                    }}
                >
                    <Button
                        onClick={onClose}
                        disabled={saving}
                    >
                        {t("common.cancel")}
                    </Button>

                    <Button
                        variant="contained"
                        onClick={() => void handleSave()}
                        disabled={
                            !settings ||
                            loading ||
                            saving
                        }
                    >
                        {saving
                            ? `${t("common.save")}...`
                            : t("common.save")}
                    </Button>
                </Box>
            </DialogActions>
        </Dialog>
    );
}