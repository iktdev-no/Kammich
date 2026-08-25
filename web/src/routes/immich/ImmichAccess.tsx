import { useEffect, useState } from 'react';
import {
    Box,
    Typography,
    Card,
    CardContent,
    Avatar,
    Chip,
    List,
    ListItem,
    ListItemText,
    ListItemIcon,
    Divider,
    CircularProgress,
    Paper,
    IconButton,
    Tooltip
} from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import PersonIcon from '@mui/icons-material/Person';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import DeleteIcon from '@mui/icons-material/Delete';
import type { ImmichUserAccesses } from '../../types/types';
import { immichAccessAll, immichDeleteApiKey } from '../../api/requests/immich';

export default function ImmichAccess() {
    const [data, setData] = useState<ImmichUserAccesses[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [deletingKeyId, setDeletingKeyId] = useState<string | null>(null);

    useEffect(() => {
        immichAccessAll()
            .then((response) => {
                setData(response);
                setLoading(false);
            })
            .catch((err) => {
                console.error('Kunne ikke hente Immich-tilganger:', err);
                setError('Klarte ikke å hente brukere og servere.');
                setLoading(false);
            });
    }, []);

    const handleDelete = async (keyId: string) => {
        if (!window.confirm('Er du sikker på at du vil slette denne API-nøkkelen?')) {
            return;
        }

        setDeletingKeyId(keyId);
        try {
            await immichDeleteApiKey(keyId);
            // Fjern den slettede nøkkelen fra lokal state for umiddelbar oppdatering
            setData((prevData) =>
                prevData
                    .map((userAccess) => ({
                        ...userAccess,
                        servers: userAccess.servers.filter((server) => server.keyId !== keyId),
                    }))
                    .filter((userAccess) => userAccess.servers.length > 0) // Valgfritt: fjerner brukeren hvis de ikke har noen servere igjen
            );
        } catch (err) {
            console.error('Klarte ikke å slette API-nøkkel:', err);
            alert('En feil oppstod under sletting av nøkkelen.');
        } finally {
            setDeletingKeyId(null);
        }
    };

    if (loading) {
        return (
            <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "50vh" }} >
                <CircularProgress sx={{ color: '#425466' }} />
            </Box>
        );
    }

    if (error) {
        return (
            <Box sx={{ p: 3 }}>
                <Typography color="error">{error}</Typography>
            </Box>
        );
    }

    return (
        <Box sx={{ maxWidth: 900, mx: 'auto', p: 3 }}>
            <Typography variant="h5" gutterBottom sx={{ color: 'text.primary', mb: 3, fontWeight: "bold" }}>
                Immich Brukere & Server-tilganger
            </Typography>

            {data.length === 0 ? (
                <Paper elevation={0} sx={{ p: 4, textAlign: 'center', backgroundColor: 'background.default', borderRadius: 3 }}>
                    <Typography color="text.secondary">Ingen registrerte Immich-brukere funnet.</Typography>
                </Paper>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    {data.map((item, index) => (
                        <Card
                            key={index}
                            elevation={0}
                            sx={{
                                borderRadius: 3,
                                border: '1px solid',
                                borderColor: 'divider',
                                backgroundColor: 'background.paper',
                                transition: 'all 0.2s ease-in-out',
                                '&:hover': {
                                    borderColor: 'primary.main',
                                }
                            }}
                        >
                            <CardContent>
                                {/* Bruker-header */}
                                <Box sx={{
                                    display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2
                                }}>
                                    <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                                        <Avatar
                                            src={`/api/v1/immich/profile-image?userId=${item.user.id}`}
                                            sx={{ bgcolor: 'primary.main', width: 48, height: 48 }}
                                        >
                                            <PersonIcon />
                                        </Avatar>
                                        <Box>
                                            <Typography variant="h6" sx={{ fontWeight: "600" }}>
                                                {item.user.name}
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary">
                                                {item.user.email}
                                            </Typography>
                                        </Box>
                                    </Box>

                                    {item.isActive && (
                                        <Chip
                                            label="Aktiv Profil"
                                            color="primary"
                                            size="small"
                                            icon={<CheckCircleIcon />}
                                            sx={{ fontWeight: 'medium' }}
                                        />
                                    )}
                                </Box>

                                <Divider sx={{ my: 2 }} />

                                {/* Server-liste */}
                                <Typography variant="subtitle2" color="text.secondary" gutterBottom sx={{ textTransform: 'uppercase', fontSize: '0.75rem', letterSpacing: 1 }}>
                                    Tilknyttede Serverer ({item.servers.length})
                                </Typography>

                                <List dense disablePadding>
                                    {item.servers.map((server, sIndex) => (
                                        <ListItem
                                            key={sIndex}
                                            sx={{
                                                py: 1,
                                                px: 2,
                                                my: 1,
                                                borderRadius: 2,
                                                backgroundColor: server.isActive ? 'action.selected' : 'action.hover',
                                                border: '1px solid',
                                                borderColor: server.isActive ? 'primary.light' : 'transparent',
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center'
                                            }}
                                        >
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexGrow: 1 }}>
                                                <ListItemIcon sx={{ minWidth: 36 }}>
                                                    <StorageIcon color={server.isActive ? 'primary' : 'disabled'} fontSize="small" />
                                                </ListItemIcon>
                                                <ListItemText
                                                    primary={
                                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                            <Typography variant="body2" sx={{ fontWeight: "medium", fontFamily: "monospace" }} >
                                                                {server.serverUrl}
                                                            </Typography>
                                                            {server.keyName && (
                                                                <Typography variant="caption" color="text.secondary">
                                                                    ({server.keyName})
                                                                </Typography>
                                                            )}
                                                        </Box>
                                                    }
                                                    secondary={`Opprettet: ${new Date(server.createdAt).toLocaleDateString()}`}
                                                />
                                            </Box>

                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                {server.isActive && (
                                                    <Chip label="Aktiv server" size="small" variant="outlined" color="primary" sx={{ height: 24, fontSize: '0.7rem' }} />
                                                )}

                                                {/* Vis sletteknapp KUN hvis serveren/nøkkelen IKKE er aktiv */}
                                                {!server.isActive && (
                                                    <Tooltip title="Slett inaktiv nøkkel">
                                                        <span>
                                                            <IconButton
                                                                size="small"
                                                                color="error"
                                                                disabled={deletingKeyId === server.keyId}
                                                                onClick={() => handleDelete(server.keyId)}
                                                            >
                                                                {deletingKeyId === server.keyId ? (
                                                                    <CircularProgress size={18} color="error" />
                                                                ) : (
                                                                    <DeleteIcon fontSize="small" />
                                                                )}
                                                            </IconButton>
                                                        </span>
                                                    </Tooltip>
                                                )}
                                            </Box>
                                        </ListItem>
                                    ))}
                                </List>
                            </CardContent>
                        </Card>
                    ))}
                </Box>
            )}
        </Box>
    );
}