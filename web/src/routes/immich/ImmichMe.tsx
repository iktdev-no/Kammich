import { useEffect, useMemo, useState } from "react";
import { useSseSelector } from "../../sse/useSseSelector";
import { alpha, Avatar, Box, Card, CardContent, Chip, LinearProgress, Stack, Typography, useTheme } from "@mui/material";
import type { ImmichUserMe, ImmichUserStatus, ImmichServerStorage } from "../../types/types";
import { formatBytes } from "../../utils/format";
import InfinityIcon from '@mui/icons-material/AllInclusive';
import DiscFullIcon from '@mui/icons-material/DiscFull';
import CloudDoneIcon from '@mui/icons-material/CloudDone';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import StorageIcon from '@mui/icons-material/Storage';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import EmailIcon from '@mui/icons-material/Email';
import PersonIcon from '@mui/icons-material/Person';
import { getAvatarColor } from "../../utils/immichColor";
import { immichStorage } from "../../api/requests/immich";

export default function ImmichMe() {
    const theme = useTheme();

    // Henter innlogget bruker fra SSE state
    const me: ImmichUserMe | undefined = useSseSelector(
        (state) => state.immichUserMe
    );

    // State for total serverlagring
    const [serverStorage, setServerStorage] = useState<ImmichServerStorage | null>(null);

    useEffect(() => {
        let isMounted = true;
        immichStorage()
            .then((data) => {
                if (isMounted) setServerStorage(data);
            })
            .catch(() => {
                if (isMounted) setServerStorage(null);
            });

        return () => {
            isMounted = false;
        };
    }, []);

    // Beregn profilbilde URL hvis den eksisterer
    const profilePictureUrl = useMemo(() => {
        if (me?.profileImagePath && me?.id) {
            return `/api/v1/immich/profile-image?userId=${me.id}`;
        }
        return undefined;
    }, [me]);

    // Beregn personlig kvote
    const hasQuota = typeof me?.quotaSizeInBytes === 'number' && me.quotaSizeInBytes > 0;
    const isUnlimited = !hasQuota;

    const usagePercent = useMemo(() => {
        if (hasQuota && me?.quotaUsageInBytes !== null && me?.quotaUsageInBytes !== undefined && me?.quotaUsageInBytes > 0) {
            return Math.min(100, (me.quotaUsageInBytes / me.quotaSizeInBytes!) * 100);
        }
        return 0;
    }, [hasQuota, me]);

    // Beregn total serverdisk-bruk fra ImmichServerStorage-modellen
    // Beregn total serverdisk-bruk med riktig feltnavn (diskUseRaw)
    const serverUsagePercent = useMemo(() => {
        if (serverStorage?.diskSizeRaw && serverStorage?.diskUseRaw && serverStorage.diskSizeRaw > 0) {
            return Math.min(100, (serverStorage.diskUseRaw / serverStorage.diskSizeRaw) * 100);
        }
        return 0;
    }, [serverStorage]);

    // Beregn hvor mange prosent av *hele serverdisken* denne brukeren bruker (hvis kvote ikke finnes, eller som sammenligning)
    const userShareOfTotalPercent = useMemo(() => {
        if (serverStorage?.diskSizeRaw && serverStorage?.diskSizeRaw > 0 && me?.quotaUsageInBytes) {
            return Math.min(100, (me.quotaUsageInBytes / serverStorage.diskSizeRaw) * 100);
        }
        return 0;
    }, [serverStorage, me]);

    if (!me) {
        return (
            <Typography variant="h6" color="text.secondary" sx={{ p: 3 }}>
                Laster brukerinformasjon...
            </Typography>
        );
    }

    const getStatusColor = (status: ImmichUserStatus | null): "success" | "error" | "warning" | "default" => {
        switch (status) {
            case "active": return "success";
            case "removing": return "warning";
            case "deleted": return "error";
            default: return "default";
        }
    };

    return (
        <Box
            sx={{
                p: { xs: 2, md: 4 },
                bgcolor: "background.default",
                minHeight: "100%",
                display: "flex",
                flexDirection: "column",
                gap: 4,
            }}
        >
            <Typography variant="h4" sx={{ fontWeight: 700, color: "text.primary" }}>
                Min Profil
            </Typography>

            {/* Overlappende profilbilde og info-kort */}
            <Box sx={{ position: "relative", mt: 8, mb: 4 }}>
                {/* Profilbilde (sirkel) */}
                <Avatar
                    src={profilePictureUrl}
                    alt={me.name}
                    sx={{
                        width: 150,
                        height: 150,
                        position: "absolute",
                        top: -75,
                        left: { xs: "50%", md: 40 },
                        transform: { xs: "translateX(-50%)", md: "none" },
                        border: `6px solid ${theme.palette.background.paper}`,
                        boxShadow: theme.shadows[10],
                        bgcolor: getAvatarColor(me.avatarColor),
                        fontSize: "3rem",
                        zIndex: 2,
                    }}
                >
                    {!profilePictureUrl && (me.name?.[0]?.toUpperCase() || <PersonIcon fontSize="inherit" />)}
                </Avatar>

                {/* Info-kort */}
                <Card
                    elevation={3}
                    sx={{
                        pt: { xs: 10, md: 3 },
                        pb: 3,
                        px: { xs: 3, md: 3 },
                        borderRadius: 4,
                        bgcolor: "background.paper",
                        display: "flex",
                        flexDirection: { xs: "column", md: "row" },
                        alignItems: { xs: "center", md: "flex-start" },
                        justifyContent: "space-between",
                        textAlign: { xs: "center", md: "left" },
                        zIndex: 1,
                    }}
                >
                    <CardContent
                        sx={{
                            flex: 1,
                            pl: { md: "200px" },
                            display: "flex",
                            flexDirection: "column",
                            gap: 1.5,
                        }}
                    >
                        <Typography variant="h5" sx={{ fontWeight: 700, color: "text.primary" }}>
                            {me.name}
                        </Typography>
                        <Stack
                            direction="row"
                            spacing={1}
                            sx={{
                                alignItems: "center",
                                justifyContent: { xs: "center", md: "flex-start" },
                                color: "text.secondary"
                            }}
                        >
                            <EmailIcon fontSize="small" />
                            <Typography variant="body1">{me.email}</Typography>
                        </Stack>
                        <Box sx={{ mt: 1 }}>
                            <Chip
                                label={`Status: ${me.status?.toUpperCase() || "UKJENT"}`}
                                color={getStatusColor(me.status)}
                                size="small"
                                variant="outlined"
                                sx={{ fontWeight: 600 }}
                            />
                        </Box>
                    </CardContent>

                    {/* Ekstra info på høyre side */}
                    <Stack
                        direction="row"
                        spacing={2}
                        sx={{
                            pr: 2,
                            pt: { xs: 2, md: 0 },
                            color: "text.secondary",
                            alignItems: "center"
                        }}
                    >
                        {me.isAdmin && (
                            <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }} title="Administrator">
                                <AdminPanelSettingsIcon color="primary" />
                                <Typography variant="caption" sx={{ fontWeight: 600, color: theme.palette.primary.main }}>ADMIN</Typography>
                            </Stack>
                        )}
                        <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }} title="Medlem siden">
                            <CalendarTodayIcon fontSize="small" />
                            <Typography variant="caption">
                                {new Date(me.createdAt).toLocaleDateString()}
                            </Typography>
                        </Stack>
                    </Stack>
                </Card>
            </Box>

            {/* Lagringskvote / Serverdisk seksjon */}
            <Card elevation={2} sx={{ borderRadius: 4, p: 2 }}>
                <CardContent>
                    <Stack direction="row" spacing={2} sx={{ alignItems: "center", mb: 2 }}>
                        <StorageIcon sx={{ fontSize: 28, color: theme.palette.info.main }} />
                        <Typography variant="h6" sx={{ fontWeight: 600 }}>
                            Lagringsinformasjon
                        </Typography>
                    </Stack>

                    {isUnlimited ? (
                        <Stack spacing={3}>
                            <Stack
                                direction="row"
                                spacing={2}
                                sx={{
                                    p: 3,
                                    bgcolor: alpha(theme.palette.success.main, 0.1),
                                    borderRadius: 3,
                                    border: `1px dashed ${theme.palette.success.main}`,
                                    alignItems: "center"
                                }}
                            >
                                <InfinityIcon sx={{ fontSize: 40, color: theme.palette.success.main }} />
                                <Box>
                                    <Typography variant="subtitle1" sx={{ fontWeight: 600, color: theme.palette.success.dark }}>
                                        Ubegrenset personlig kvote
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Kontoen din har ingen individuell lagringsgrense.
                                    </Typography>
                                </Box>
                            </Stack>

                            {/* Serverdisk-info */}
                            {serverStorage?.diskSizeRaw && (
                                <Stack spacing={1.5} sx={{ mt: 2, pt: 2, borderTop: `1px solid ${theme.palette.divider}` }}>
                                    <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center" }}>
                                        <Typography variant="body2" color="text.secondary">
                                            Total serverdisk: {formatBytes(serverStorage.diskUseRaw || 0)} av {formatBytes(serverStorage.diskSizeRaw)} brukt
                                        </Typography>
                                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                            {serverUsagePercent.toFixed(0)}% brukt
                                        </Typography>
                                    </Stack>

                                    {/* Overlappende progress-barer (Android-stil) */}
                                    <Box sx={{ position: "relative", height: 8, width: "100%" }}>
                                        {/* Bakre bar: Total diskbruk på serveren (nedtonet) */}
                                        <LinearProgress
                                            variant="determinate"
                                            value={serverUsagePercent}
                                            sx={{
                                                position: "absolute",
                                                top: 0,
                                                left: 0,
                                                right: 0,
                                                height: 8,
                                                borderRadius: 4,
                                                bgcolor: theme.palette.background.default,
                                                '& .MuiLinearProgress-bar': {
                                                    borderRadius: 4,
                                                    bgcolor: alpha(theme.palette.info.dark, 1), // Lysere/nedtonet for totalen
                                                }
                                            }}
                                        />
                                        {/* Fremre bar: Din egen bruk / eller fremhevet primærfarge */}
                                        <LinearProgress
                                            variant="determinate"
                                            value={userShareOfTotalPercent + 20}
                                            sx={{
                                                position: "absolute",
                                                top: 0,
                                                left: 0,
                                                right: 0,
                                                height: 8,
                                                borderRadius: 4,
                                                bgcolor: "transparent", // Gjennomsiktig bakgrunn så den bakre syner gjennom
                                                '& .MuiLinearProgress-bar': {
                                                    borderRadius: 4,
                                                    bgcolor: theme.palette.success.main, // Sterkere farge for din del
                                                }
                                            }}
                                        />
                                    </Box>
                                </Stack>
                            )}
                        </Stack>
                    ) : (
                        <Stack spacing={2}>
                            <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center" }}>
                                <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                                    <CloudDoneIcon color="success" fontSize="small" />
                                    <Typography variant="body2" color="text.secondary">
                                        Bruker {formatBytes(me.quotaUsageInBytes || 0)} av {formatBytes(me.quotaSizeInBytes!)}
                                    </Typography>
                                </Stack>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                    {usagePercent.toFixed(0)}% brukt
                                </Typography>
                            </Stack>

                            <LinearProgress
                                variant="determinate"
                                value={usagePercent}
                                sx={{
                                    height: 10,
                                    borderRadius: 5,
                                    bgcolor: theme.palette.grey[200],
                                    '& .MuiLinearProgress-bar': {
                                        borderRadius: 5,
                                        bgcolor: usagePercent > 90 ? theme.palette.error.main : theme.palette.info.main
                                    }
                                }}
                            />

                            {usagePercent > 90 && (
                                <Typography variant="caption" color="error" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                    <DiscFullIcon fontSize="inherit" /> Du nærmer deg lagringsgrensen. Du kan kanskje ikke laste opp flere bilder.
                                </Typography>
                            )}
                        </Stack>
                    )}

                    {me.storageLabel && (
                        <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
                            Lagringsetikett: {me.storageLabel}
                        </Typography>
                    )}
                </CardContent>
            </Card>
        </Box>
    );
}