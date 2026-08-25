import { useState, useEffect } from "react";
import {
    Box, Typography, Avatar, CircularProgress,
    Dialog, IconButton, useTheme, Chip, Stack, Slide
} from "@mui/material";
import type { TransitionProps } from '@mui/material/transitions';
import AddIcon from '@mui/icons-material/Add';
import CloseIcon from '@mui/icons-material/Close';
import StorageIcon from '@mui/icons-material/Storage';
import type { ImmichUserAccesses, ImmichUserMe } from "../../types/types";
import { immichAccessAll, immichChangeUser } from "../../api/requests/immich";
import { getAvatarColor } from "../../utils/immichColor";
import ImmichLogin from "../../components/immich/ImmichLogin";
import React from "react";

const Transition = React.forwardRef(function Transition(
    props: TransitionProps & { children: React.ReactElement },
    ref: React.Ref<unknown>,
) {
    return <Slide direction="up" ref={ref} {...props} />;
});

interface ImmichProfilesProps {
    onLoginSuccess?: (login: ImmichUserMe) => void;
}

export default function ImmichProfiles({ }: ImmichProfilesProps) {
    const theme = useTheme();
    const [profiles, setProfiles] = useState<ImmichUserAccesses[]>([]);
    const [loading, setLoading] = useState(true);
    const [openAddDialog, setOpenAddDialog] = useState(false);
    const [swappingId, setSwappingId] = useState<string | null>(null);

    useEffect(() => {
        loadProfiles();
    }, []);

    const loadProfiles = async () => {
        try {
            setLoading(true);
            const data = await immichAccessAll();
            setProfiles(data || []);
        } catch (err) {
            console.error("Klarte ikke å hente profiler:", err);
        } finally {
            setLoading(false);
        }
    };

    const handleSelectUser = async (userId: string) => {
        try {
            setSwappingId(userId);
            const success = await immichChangeUser(userId); // Bruker din definerte funksjon

            if (success) {
                loadProfiles();
            } else {
                throw new Error("Kunne ikke bytte til bruker");
            }
        } catch (err) {
            console.error("Feil ved bytte:", err);
        } finally {
            setSwappingId(null);
        }
    };

    if (loading) return <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "50vh" }}><CircularProgress /></Box>;

    return (
        <Box sx={{ p: { xs: 3, md: 6 }, maxWidth: 1100, mx: "auto" }}>
            <Typography variant="h3" sx={{ fontWeight: 800, mb: 6, textAlign: "center" }}>Hvem er fotografen?</Typography>

            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 4, justifyContent: "center" }}>
                {profiles.map(({ user, servers }) => {
                    const isSwapping = swappingId === user.id;

                    return (
                        <Box key={user.id} sx={{ width: 160, display: "flex", flexDirection: "column", alignItems: "center" }}>
                            <Avatar
                                onClick={() => !swappingId && handleSelectUser(user.id)}
                                src={user.profileImagePath && !isSwapping ? `/api/v1/immich/profile-image?userId=${user.id}` : undefined}
                                sx={{
                                    width: 130, height: 130, mb: 2,
                                    cursor: swappingId ? "wait" : "pointer",
                                    bgcolor: getAvatarColor(user.avatarColor),
                                    border: "4px solid transparent",
                                    transition: "all 0.2s",
                                    opacity: isSwapping ? 0.6 : 1,
                                    '&:hover': {
                                        transform: swappingId ? "none" : "scale(1.05)",
                                        borderColor: "primary.main"
                                    }
                                }}
                            >
                                {isSwapping ? (
                                    <CircularProgress size={40} sx={{ color: "white" }} />
                                ) : (
                                    user.name[0].toUpperCase()
                                )}
                            </Avatar>

                            <Typography variant="h6" sx={{ fontWeight: 600 }}>{user.name}</Typography>

                            <Stack spacing={0.5} sx={{ mt: 1, width: "100%", alignItems: "center" }}>
                                {servers.map((s) => (
                                    <Chip key={s.keyId} label={s.serverUrl} size="small" variant="outlined"
                                        icon={<StorageIcon fontSize="small" />} sx={{ fontSize: "0.7rem", maxWidth: "100%" }} />
                                ))}
                            </Stack>
                        </Box>
                    );
                })}

                <Box sx={{ width: 160, display: "flex", flexDirection: "column", alignItems: "center" }}>
                    <Avatar
                        onClick={() => setOpenAddDialog(true)}
                        sx={{ width: 130, height: 130, mb: 2, cursor: "pointer", border: `4px dashed ${theme.palette.divider}`, bgcolor: "transparent", color: "text.secondary" }}
                    ><AddIcon sx={{ fontSize: "3rem" }} /></Avatar>
                    <Typography variant="h6" sx={{ color: "text.secondary" }}>Legg til</Typography>
                </Box>
            </Box>

            <Dialog
                fullScreen
                open={openAddDialog}
                onClose={() => setOpenAddDialog(false)}
                slots={{
                    transition: Transition,
                }}
                slotProps={{
                    paper: {
                        sx: { bgcolor: "#000000" }
                    }
                }}
            >
                <IconButton
                    onClick={() => setOpenAddDialog(false)}
                    sx={{ position: "absolute", top: 16, right: 16, zIndex: 10, color: "white" }}
                >
                    <CloseIcon fontSize="large" />
                </IconButton>

                <Box sx={{
                    minHeight: "100vh",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    p: 2
                }}>
                    <ImmichLogin onLoginSuccess={() => { setOpenAddDialog(false); loadProfiles(); }} />
                </Box>
            </Dialog>
        </Box>
    );
}