import { useEffect, useState } from "react";
import {
    Alert,
    Box,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Container,
    Grid,
    IconButton,
    Stack,
    Typography,
} from "@mui/material";
import PowerSettingsNewIcon from "@mui/icons-material/PowerSettingsNew";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import StorageIcon from "@mui/icons-material/Storage";

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
import { useWarningDialog } from "../hooks/useWarningDialog";
import DiskHealthOverviewCard from "../features/system/DiskHealthOverviewCard";
import LanguageSelectionCard from "../features/system/LanguageSelectionCard";

export function System() {
    const { t } = useTranslation();

    const {
        showWarning,
        warningDialog,
    } = useWarningDialog();

    const [permissions, setPermissions] =
        useState<PowerPermissionsDto | null>(null);

    const [actionLoading, setActionLoading] =
        useState<boolean>(false);

    const [feedback, setFeedback] = useState<{
        message: string;
        severity: "success" | "error";
    } | null>(null);

    const [activePowerAction, setActivePowerAction] =
        useState<"poweroff" | "reboot" | null>(null);

    const diskHealth = useSseSelector(
        state => state.diskHealth
    );

    const fetchPermissions = (): void => {
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
            });
    };

    useEffect(() => {
        fetchPermissions();
    }, []);

    const executePowerAction = async (
        action: "poweroff" | "reboot"
    ): Promise<void> => {
        setActionLoading(true);
        setActivePowerAction(action);
        setFeedback(null);

        try {
            const response =
                action === "poweroff"
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
            setActivePowerAction(null);
        }
    };

    const confirmPowerAction = (
        action: "poweroff" | "reboot"
    ): void => {
        const isPowerOff =
            action === "poweroff";

        showWarning({
            severity: isPowerOff
                ? "critical"
                : "warning",

            title: isPowerOff
                ? t(
                    "system.actions.shutdown_confirm"
                )
                : t(
                    "system.actions.reboot_confirm"
                ),

            message: isPowerOff
                ? t(
                    "system.actions.confirm.shutdown"
                )
                : t(
                    "system.actions.confirm.reboot"
                ),

            onConfirm: () =>
                executePowerAction(action),
        });
    };

    const disks = diskHealth ?? [];

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
                permissions === null
                    ? null
                    : (
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
                                        disabled={
                                            actionLoading
                                        }
                                        onClick={() =>
                                            confirmPowerAction(
                                                "reboot"
                                            )
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
                                        {actionLoading &&
                                            activePowerAction ===
                                            "reboot" ? (
                                            <CircularProgress
                                                size={24}
                                                color="inherit"
                                            />
                                        ) : (
                                            <RestartAltIcon />
                                        )}
                                    </IconButton>
                                )}

                                {canPowerOff && (
                                    <IconButton
                                        color="error"
                                        disabled={
                                            actionLoading
                                        }
                                        onClick={() =>
                                            confirmPowerAction(
                                                "poweroff"
                                            )
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
                                        {actionLoading &&
                                            activePowerAction ===
                                            "poweroff" ? (
                                            <CircularProgress
                                                size={24}
                                                color="inherit"
                                            />
                                        ) : (
                                            <PowerSettingsNewIcon />
                                        )}
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

                    <DiskHealthOverviewCard disks={diskHealth} />

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
                                            color:
                                                "text.disabled",
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
            </Container>
            <LanguageSelectionCard />
            {warningDialog}
        </PageLayout>
    );
}