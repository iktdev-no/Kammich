import { Popover, Box, Typography, Button, IconButton, Badge, Tooltip } from "@mui/material";
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import CheckIcon from '@mui/icons-material/Check';
import { useMemo, useState } from "react";
import { useSseSelector } from "../sse/useSseSelector";
import { formatNotificationTime } from "../utils/format";
import type { Notification } from "../types/types";

export default function NotificationPopover() {
    const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);

    // --- Hent notifications direkte fra SSE ---
    // useSseSelector henter nå state direkte. Vi cacher filtreringen med useMemo.
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
        return 'primary'; // Blå dot/tall for info
    };

    return (
        <>
            <IconButton color="inherit" onClick={handleClick}>
                <Badge
                    badgeContent={activeNotifications.length > 0 ? activeNotifications.length : null}
                    color={getBadgeColor()}
                    // Her fjerner vi dotten hvis det ikke er noen varsler
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
                slotProps={{ paper: { sx: { width: 320, mt: 1.5, borderRadius: 3, boxShadow: 3 } } }}
            >
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="h6">Notifications</Typography>
                    <Button startIcon={<CheckIcon />} size="small" sx={{ textTransform: 'none' }}>
                        Mark all
                    </Button>
                </Box>

                {activeNotifications.map(n => <NotificationItem n={n} />)}
            </Popover>
        </>
    );
}

function NotificationItem({ n }: { n: Notification }) {
    const { relativeTime, exactTime } = formatNotificationTime(n.createdAt);

    return (
        <Box key={n.id} sx={{ p: 1.5, '&:hover': { bgcolor: 'action.hover', borderRadius: 1 } }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="subtitle2" sx={{
                    fontWeight: 600
                }}>{n.title}</Typography>
                {/* Her kommer den relative tiden */}
                <Tooltip title={exactTime}>
                    <Typography variant="caption" color="text.secondary" sx={{ cursor: 'help' }}>
                        {relativeTime}
                    </Typography>
                </Tooltip>
            </Box>
            <Typography variant="body2" color="text.secondary">{n.message}</Typography>
        </Box>
    );
}