import { useEffect, useState } from "react";

import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Container,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Grid,
    IconButton,
    Stack,
    Typography,
} from "@mui/material";

import PowerSettingsNewIcon from "@mui/icons-material/PowerSettingsNew";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import StorageIcon from "@mui/icons-material/Storage";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutlineOutlined";

import type {
    PowerPermissionsDto,
} from "../types/types";

import {
    executePowerOff,
    executeReboot,
    getPowerPermissions,
} from "../api/requests/system";

import { useTranslation } from "react-i18next";

import { useSseSelector } from "../hooks/useSseSelector";

import DiskHealthCard from "../features/system/DiskHealthCard";

import PageLayout from "../components/layouts/PageLayout";

export function System() {
    const { t } = useTranslation();

    const [permissions, setPermissions] =
        useState<PowerPermissionsDto | null>(null);

    const [loading, setLoading] =
        useState<boolean>(true);

    const [actionLoading, setActionLoading] =
        useState<boolean>(false);

    const [feedback, setFeedback] = useState<{
        message: string;
        severity: "success" | "error";
    } | null>(null);

    const [confirmAction, setConfirmAction] =
        useState<"poweroff" | "reboot" | null>(null);

    const diskHealth = useSseSelector(
        state => state.diskHealth
    );

    const fetchPermissions = (): void => {
        setLoading(true);

        getPowerPermissions()
            .then(res => {
                setPermissions(res);
            })
            .catch((error: unknown) => {
                console.error(
                    "Klarte ikke å hente strømtillatelser",
                    error
                );

                setFeedback({
                    message: t(
                        "common.system_permission_error1"
                    ),
                    severity: "error",
                });
            })
            .finally(() => {
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchPermissions();
    }, []);

    const handleExecute = async (): Promise<void> => {
        if (confirmAction === null) {
            return;
        }

        setActionLoading(true);
        setFeedback(null);

        try {
            const response =
                confirmAction === "poweroff"
                    ? await executePowerOff()
                    : await executeReboot();

            setFeedback({
                message: response.message,
                severity: response.success
                    ? "success"
                    : "error",
            });
        } catch (error: unknown) {
            console.error(
                "Feil ved utførelse av strømkommando",
                error
            );

            const message =
                error instanceof Error
                    ? error.message
                    : t("common.unexpected_error");

            setFeedback({
                message,
                severity: "error",
            });
        } finally {
            setActionLoading(false);
            setConfirmAction(null);
        }
    };

    const disks = diskHealth ?? [];

    const healthyDisks = disks.filter(
        disk => disk.isHealthy
    ).length;

    const unhealthyDisks =
        disks.length - healthyDisks;

    const canReboot =
        permissions?.canReboot === true;

    const canPowerOff =
        permissions?.canPowerOff === true;

    const canPerformPowerAction =
        canReboot || canPowerOff;

    return (
        <PageLayout
            title={t("system.title")}
            actions={
                permissions === null ? null : (
                    canPerformPowerAction && (
                        <Stack
                            direction="row"
                            spacing={1.5}
                            sx={{
                                alignItems: "center",
                            }}
                        >
                            {canReboot && (
                                <IconButton
                                    color="warning"
                                    disabled={actionLoading}
                                    onClick={() =>
                                        setConfirmAction("reboot")
                                    }
                                    sx={{
                                        width: 48,
                                        height: 48,
                                        backgroundColor:
                                            "warning.main",
                                        color:
                                            "warning.contrastText",
                                        "&:hover": {
                                            backgroundColor:
                                                "warning.dark",
                                        },
                                    }}
                                >
                                    <RestartAltIcon />
                                </IconButton>
                            )}

                            {canPowerOff && (
                                <IconButton
                                    color="error"
                                    disabled={actionLoading}
                                    onClick={() =>
                                        setConfirmAction("poweroff")
                                    }
                                    sx={{
                                        width: 48,
                                        height: 48,
                                        backgroundColor:
                                            "error.main",
                                        color:
                                            "error.contrastText",
                                        "&:hover": {
                                            backgroundColor:
                                                "error.dark",
                                        },
                                    }}
                                >
                                    <PowerSettingsNewIcon />
                                </IconButton>
                            )}
                        </Stack>
                    )
                )
            }
        >
            {!canPerformPowerAction && (
                <Alert
                    severity="warning"
                    icon={<WarningAmberIcon />}
                    sx={{
                        py: 0,
                        alignItems: "center",
                    }}
                >
                    {t(
                        "system.actions.not_permitted"
                    )}
                </Alert>
            )}
            <Container
                maxWidth="lg"
                sx={{
                    py: {
                        xs: 2,
                        sm: 3,
                        md: 4,
                    },
                }}
            >

                <Stack spacing={3}>
                    {feedback && (
                        <Alert
                            severity={feedback.severity}
                            onClose={() =>
                                setFeedback(null)
                            }
                        >
                            {feedback.message}
                        </Alert>
                    )}

                    <Card
                        sx={{
                            borderRadius: 3,
                        }}
                    >
                        <CardContent
                            sx={{
                                p: {
                                    xs: 2.5,
                                    sm: 3,
                                },
                                "&:last-child": {
                                    pb: {
                                        xs: 2.5,
                                        sm: 3,
                                    },
                                },
                            }}
                        >
                            <Stack spacing={2}>
                                <Typography
                                    variant="h6"
                                    sx={{
                                        fontWeight: 600,
                                    }}
                                >
                                    Disk health
                                </Typography>

                                <Stack
                                    direction={{
                                        xs: "column",
                                        sm: "row",
                                    }}
                                    spacing={2}
                                >
                                    <Box
                                        sx={{
                                            flex: 1,
                                            minWidth: 0,
                                        }}
                                    >
                                        <Stack
                                            direction="row"
                                            spacing={1.5}
                                            sx={{
                                                alignItems:
                                                    "center",
                                            }}
                                        >
                                            {unhealthyDisks ===
                                                0 ? (
                                                <CheckCircleIcon
                                                    color="success"
                                                />
                                            ) : (
                                                <ErrorOutlineIcon
                                                    color="warning"
                                                />
                                            )}

                                            <Box>
                                                <Typography
                                                    sx={{
                                                        fontWeight:
                                                            600,
                                                    }}
                                                >
                                                    {unhealthyDisks ===
                                                        0
                                                        ? "All disks healthy"
                                                        : "Disk health warning"}
                                                </Typography>

                                                <Typography
                                                    variant="body2"
                                                    color="text.secondary"
                                                >
                                                    {disks.length}{" "}
                                                    {disks.length ===
                                                        1
                                                        ? "disk"
                                                        : "disks"}{" "}
                                                    detected
                                                </Typography>
                                            </Box>
                                        </Stack>
                                    </Box>

                                    <Box
                                        sx={{
                                            display: "flex",
                                            alignItems:
                                                "center",
                                            justifyContent: {
                                                xs: "flex-start",
                                                sm: "flex-end",
                                            },
                                        }}
                                    >
                                        <Chip
                                            icon={
                                                unhealthyDisks ===
                                                    0 ? (
                                                    <CheckCircleIcon />
                                                ) : (
                                                    <ErrorOutlineIcon />
                                                )
                                            }
                                            label={
                                                unhealthyDisks ===
                                                    0
                                                    ? `${healthyDisks} healthy`
                                                    : `${unhealthyDisks} warning`
                                            }
                                            color={
                                                unhealthyDisks ===
                                                    0
                                                    ? "success"
                                                    : "warning"
                                            }
                                            variant="outlined"
                                        />
                                    </Box>
                                </Stack>
                            </Stack>
                        </CardContent>
                    </Card>

                    <Box>
                        <Typography
                            variant="h6"
                            sx={{
                                fontWeight: 600,
                                mb: 1.5,
                            }}
                        >
                            Disk health
                        </Typography>

                        {disks.length === 0 ? (
                            <Card
                                sx={{
                                    borderRadius: 3,
                                }}
                            >
                                <CardContent
                                    sx={{
                                        p: 3,
                                        textAlign: "center",
                                    }}
                                >
                                    <StorageIcon
                                        sx={{
                                            fontSize: 40,
                                            color: "text.disabled",
                                            mb: 1,
                                        }}
                                    />

                                    <Typography
                                        color="text.secondary"
                                    >
                                        No disk health information
                                        available
                                    </Typography>
                                </CardContent>
                            </Card>
                        ) : (
                            <Grid
                                container
                                spacing={2}
                            >
                                {disks.map(
                                    disk => (
                                        <Grid
                                            key={
                                                disk.deviceName
                                            }
                                            size={{
                                                xs: 12,
                                                sm: 6,
                                                lg: 4,
                                            }}
                                        >
                                            <DiskHealthCard
                                                health={disk}
                                            />
                                        </Grid>
                                    )
                                )}
                            </Grid>
                        )}
                    </Box>
                </Stack>

                <Dialog
                    open={confirmAction !== null}
                    onClose={() => {
                        if (!actionLoading) {
                            setConfirmAction(null);
                        }
                    }}
                    fullWidth
                    maxWidth="xs"
                >
                    <DialogTitle
                        sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1.5,
                        }}
                    >
                        <WarningAmberIcon color="warning" />

                        {confirmAction === "poweroff"
                            ? t(
                                "system.actions.shutdown_confirm"
                            )
                            : t(
                                "system.actions.reboot_confirm"
                            )}
                    </DialogTitle>

                    <DialogContent>
                        <DialogContentText>
                            {confirmAction === "poweroff"
                                ? t(
                                    "system.actions.confirm.shutdown"
                                )
                                : t(
                                    "system.actions.confirm.reboot"
                                )}
                        </DialogContentText>
                    </DialogContent>

                    <DialogActions
                        sx={{
                            p: 2,
                            pt: 0,
                            gap: 1,
                        }}
                    >
                        <Button
                            onClick={() =>
                                setConfirmAction(null)
                            }
                            color="inherit"
                            disabled={actionLoading}
                            sx={{
                                minHeight: 48,
                            }}
                        >
                            {t("common.cancel")}
                        </Button>

                        <Button
                            onClick={handleExecute}
                            variant="contained"
                            color={
                                confirmAction === "poweroff"
                                    ? "error"
                                    : "warning"
                            }
                            disabled={actionLoading}
                            autoFocus
                            sx={{
                                minHeight: 48,
                            }}
                        >
                            {actionLoading ? (
                                <CircularProgress
                                    size={20}
                                    color="inherit"
                                />
                            ) : (
                                t(
                                    "system.actions.confirm.title"
                                )
                            )}
                        </Button>
                    </DialogActions>
                </Dialog>
            </Container>
        </PageLayout>
    );
}