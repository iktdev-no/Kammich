import { Popover, Box, Typography, Button, IconButton, Badge, Tooltip, Divider, useTheme } from "@mui/material";
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import CheckIcon from '@mui/icons-material/Check';
import { useMemo, useState } from "react";
import { useSseSelector } from "../hooks/useSseSelector";
import { formatNotificationTime } from "../utils/format";
import type { Notification, NotificationKey } from "../types/types";
import { dismissNotification, dismissNotifications } from "../api/requests/notifications";
import DoneAllIcon from '@mui/icons-material/DoneAll';
import CircleNotificationsIcon from '@mui/icons-material/CircleNotifications';
import { useTranslation } from "react-i18next";

export default function NotificationPopover() {
    const { t } = useTranslation()
    const theme = useTheme()
    const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);

    // --- Hent notifications direkte fra SSE ---
    const allNotifications = useSseSelector(state => state.notifications);
    const activeNotifications = useMemo(() =>
        allNotifications.filter(n => !n.dismissed),
        [allNotifications]
    );

    // Popover controls
    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => setAnchorEl(event.currentTarget);
    const handleClose = () => setAnchorEl(null);
    const open = Boolean(anchorEl);

    // --- Logikk for Badge farge ---
    const getBadgeColor = () => {
        if (activeNotifications.some(n => n.severity === 'Error')) return 'error';
        if (activeNotifications.some(n => n.severity === 'Warning')) return 'warning';
        return 'primary';
    };

    const handleDismissAll = async () => {
        try {
            await dismissNotifications();
        } catch (err) {
            console.error("Klarte ikke å fjerne alle notifikasjoner:", err);
        }
    };

    return (
        <>
            <IconButton color="inherit" onClick={handleClick}>
                <Badge
                    badgeContent={activeNotifications.length > 0 ? activeNotifications.length : null}
                    color={getBadgeColor()}
                    invisible={activeNotifications.length === 0}
                >
                    <NotificationsNoneIcon />
                </Badge>
            </IconButton>

            <Popover
                open={open}
                anchorEl={anchorEl}
                onClose={handleClose}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                slotProps={{ paper: { sx: { width: 320, mt: 1.5, borderRadius: 3, boxShadow: 3, bgcolor: theme.palette.background.default } } }}
            >
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>{t('notifications.title')}</Typography>
                    {activeNotifications.length > 0 && (
                        <Button
                            startIcon={<DoneAllIcon />}
                            size="small"
                            sx={{ textTransform: 'none', pl: 1, pr: 1 }}
                            onClick={handleDismissAll}
                        >
                            {t('notifications.dismiss_all')}
                        </Button>
                    )}
                </Box>
                <Divider sx={{ mb: 1, borderColor: theme.palette.background.default }} />
                <Box sx={{ maxHeight: 350, overflowY: 'auto', px: 1, pb: 1 }}>
                    {activeNotifications.length === 0 ? (
                        <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center", p: 3 }}>
                            <CircleNotificationsIcon sx={{
                                color: theme.palette.grey[600]
                            }} />
                            <Typography variant="body2" color="text.secondary" sx={{
                                textAlign: 'center', pt: 1,
                                color: theme.palette.grey[400]
                            }}>
                                {t('notifications.empty')}
                            </Typography>
                        </Box>
                    ) : (
                        activeNotifications.map(n => <NotificationItem key={n.id} n={n} />)
                    )}
                </Box>
            </Popover>
        </>
    );
}


export interface NotificationText {
    title: string;
    message: string;
}

const notificationTranslations = {
    ImportCompleted: {
        title: "notifications.notificationKeys.ImportCompleted.title",
        message: "notifications.notificationKeys.ImportCompleted.message",
    },

    ImportFailed: {
        title: "notifications.notificationKeys.ImportFailed.title",
        message: "notifications.notificationKeys.ImportFailed.message",
    },

    ImportNoNewFiles: {
        title: "notifications.notificationKeys.ImportNoNewFiles.title",
        message: "notifications.notificationKeys.ImportNoNewFiles.message",
    },

    ImportDeviceNotFullyAdded: {
        title: "notifications.notificationKeys.ImportDeviceNotFullyAdded.title",
        message: "notifications.notificationKeys.ImportDeviceNotFullyAdded.message",
    },

    CameraDisconnected: {
        title: "notifications.notificationKeys.CameraDisconnected.title",
        message: "notifications.notificationKeys.CameraDisconnected.message",
    },

    CameraCleanupCompleted: {
        title: "notifications.notificationKeys.CameraCleanupCompleted.title",
        message: "notifications.notificationKeys.CameraCleanupCompleted.message",
    },

    CameraCleanupFailed: {
        title: "notifications.notificationKeys.CameraCleanupFailed.title",
        message: "notifications.notificationKeys.CameraCleanupFailed.message",
    },

    CameraConnected: {
        title: "notifications.notificationKeys.CameraConnected.title",
        message: "notifications.notificationKeys.CameraConnected.message",
    },

    CameraCleanupNoFiles: {
        title: "notifications.notificationKeys.CameraCleanupNoFiles.title",
        message: "notifications.notificationKeys.CameraCleanupNoFiles.message",
    },

    CameraCleanupDisconnected: {
        title: "notifications.notificationKeys.CameraCleanupDisconnected.title",
        message: "notifications.notificationKeys.CameraCleanupDisconnected.message",
    },

    CameraDCIMMissing: {
        title: "notifications.notificationKeys.CameraDCIMMissing.title",
        message: "notifications.notificationKeys.CameraDCIMMissing.message",
    },

    SystemCreationFailureFolder: {
        title: "notifications.notificationKeys.SystemCreationFailureFolder.title",
        message: "notifications.notificationKeys.SystemCreationFailureFolder.message",
    },

    SystemWriteFailureFolder: {
        title: "notifications.notificationKeys.SystemWriteFailureFolder.title",
        message: "notifications.notificationKeys.SystemWriteFailureFolder.message",
    },

    SystemUnknownFailureFolder: {
        title: "notifications.notificationKeys.SystemUnknownFailureFolder.title",
        message: "notifications.notificationKeys.SystemUnknownFailureFolder.message",
    },

    ImportJobClaimFailed: {
        title: "notifications.notificationKeys.ImportJobClaimFailed.title",
        message: "notifications.notificationKeys.ImportJobClaimFailed.message",
    },
} as const satisfies Record<NotificationKey, {
    title: string;
    message: string;
}>;

function useNotificationText(n: Notification): NotificationText {
    const { t } = useTranslation();

    const translation = notificationTranslations[n.key];

    return {
        title: t(translation.title),
        message: t(translation.message, n.messageArgs),
    };
}


function NotificationItem({ n }: { n: Notification }) {
    const { relativeTime, exactTime } = formatNotificationTime(n.createdAt);
    const [loading, setLoading] = useState(false);

    const nt = useNotificationText(n)
    const handleDismiss = async (e: React.MouseEvent) => {
        e.stopPropagation(); // Unngå at klikket bobler opp
        if (!n.dismissable) return;

        setLoading(true);
        try {
            await dismissNotification(n.id);
        } catch (err) {
            console.error(`Klarte ikke å fjerne notifikasjon ${n.id}:`, err);
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                p: 1.5,
                mb: 0.5,
                borderRadius: 2,
                transition: 'background-color 0.2s',
                '&:hover': {
                    bgcolor: 'action.hover',
                    '& .dismiss-btn': { opacity: 1 }
                },
                position: 'relative'
            }}
        >
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                    {nt.title}
                </Typography>
                <Typography variant="subtitle2">•</Typography>
                <Tooltip title={exactTime}>
                    <Typography variant="caption" color="text.secondary" sx={{ cursor: 'help', whiteSpace: 'nowrap' }}>
                        {relativeTime}
                    </Typography>
                </Tooltip>

                {n.dismissable && (
                    <IconButton
                        className="dismiss-btn"
                        size="small"
                        onClick={handleDismiss}
                        disabled={loading}
                        sx={{
                            opacity: { xs: 1, sm: 0 }, // Alltid synlig på mobil, fade-in på desktop hover
                            transition: 'opacity 0.2s',
                            p: 0.5,
                            ml: -0.5
                        }}
                    >
                        <CheckIcon fontSize="small" />
                    </IconButton>
                )}
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-word' }}>
                {nt.message}
            </Typography>
        </Box>
    );
}