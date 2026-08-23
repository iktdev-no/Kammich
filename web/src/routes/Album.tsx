import { useState, useEffect } from "react";
import {
    Box, Typography, Fab, Card, CardMedia, CardContent,
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, IconButton, CircularProgress, Tooltip
} from "@mui/material";
import Grid from "@mui/material/Grid";
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import PhotoAlbumIcon from '@mui/icons-material/PhotoAlbum';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import SyncIcon from '@mui/icons-material/Sync';
import { getAlbums, createAlbum, updateAlbum, deleteAlbum } from "../api/requests/album";
import { syncAlbumWithFIles } from "../api/requests/album"; // Sørg for at stien matcher der funksjonen din ligger
import type { Album } from "../types/types";
import { getPhotoUrl } from "../api/requests/photo";
import { toast } from "react-toastify";
import { AlbumDialog } from "../components/album/CreateAndEditAlbumDialog";
import { useSseSelector } from "../sse/useSseSelector";

// --- HOVEDKOMPONENT ---
export function Album() {
    const user = useSseSelector(state => state.immichUserMe)
    const [albums, setAlbums] = useState<Album[]>([]);
    const [loading, setLoading] = useState(true);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingAlbum, setEditingAlbum] = useState<Album | null>(null);

    // State for slette-dialogen
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [albumToDelete, setAlbumToDelete] = useState<{ id: number; title: string } | null>(null);

    // Holder styr på hvilke album som synkroniseres akkurat nå (for animasjons-spinner på knappen)
    const [syncingId, setSyncingId] = useState<number | null>(null);

    const fetchAlbums = async () => {
        try {
            setLoading(true);
            const data = await getAlbums();
            setAlbums(data);
        } catch (err) {
            console.error("Klarte ikke å hente album:", err);
            toast.error("Klarte ikke å hente album");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchAlbums(); }, []);

    const handleSave = async (formData: any) => {
        if (!user) return;
        try {
            if (editingAlbum) {
                await updateAlbum(editingAlbum.id, {
                    ...formData,
                    use: formData.use !== undefined ? formData.use : editingAlbum.use
                });
                toast.success("Album oppdatert!");
            } else {
                await createAlbum({ ...formData, use: false });
                toast.success("Album opprettet!");
            }
            setOpenDialog(false);
            setEditingAlbum(null);
            fetchAlbums();
        } catch (err) {
            console.error("Feil ved lagring:", err);
            toast.error("Feil ved lagring av album");
        }
    };

    const openDeleteDialog = (album: Album, e: React.MouseEvent) => {
        e.stopPropagation();
        setAlbumToDelete({ id: album.id, title: album.title });
        setDeleteModalOpen(true);
    };

    const confirmDelete = async () => {
        if (!albumToDelete) return;
        try {
            await deleteAlbum(albumToDelete.id);
            toast.success("Album slettet fra Kammich");
            setDeleteModalOpen(false);
            setAlbumToDelete(null);
            fetchAlbums();
        } catch (err) {
            console.error("Feil ved sletting:", err);
            toast.error("Klarte ikke å slette album");
        }
    };

    const handleToggleActive = async (album: Album, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!user) return;
        try {
            const newUseState = !album.use;
            await updateAlbum(album.id, {
                albumName: album.title,
                description: album.description,
                startDate: album.startDate,
                endDate: album.endDate,
                use: newUseState
            });
            fetchAlbums();
        } catch (err) {
            console.error("Feil ved oppdatering:", err);
        }
    };

    const handleSync = async (albumId: number, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!user) return;
        try {
            setSyncingId(albumId);
            await syncAlbumWithFIles(albumId);
            toast.success("Album synkronisert med filer!");
            fetchAlbums();
        } catch (err) {
            console.error("Feil ved synkronisering:", err);
            toast.error("Klarte ikke å synkronisere album");
        } finally {
            setSyncingId(null);
        }
    };

    return (
        <Box sx={{ p: 3, position: 'relative', minHeight: '80vh' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 600 }}>Albums</Typography>

            {loading ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 12, color: 'text.secondary' }}>
                    <Box sx={{ position: 'relative', display: 'inline-flex', mb: 2 }}>
                        <CircularProgress size={68} thickness={4} />
                        <Box
                            sx={{
                                top: 0,
                                left: 0,
                                bottom: 0,
                                right: 0,
                                position: 'absolute',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            <PhotoAlbumIcon color="action" />
                        </Box>
                    </Box>
                    <Typography variant="h6">Laster inn album...</Typography>
                </Box>
            ) : albums.length === 0 ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 8, color: 'text.secondary' }}>
                    <PhotoAlbumIcon sx={{ fontSize: 64, mb: 2, opacity: 0.5 }} />
                    <Typography variant="h6">Ingen album ennå</Typography>
                    {user && (
                        <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setEditingAlbum(null); setOpenDialog(true); }} sx={{ mt: 3 }}>
                            Opprett album
                        </Button>
                    )}
                </Box>
            ) : (
                <Grid container spacing={3}>
                    {albums.map((album) => (
                        <Grid size={{ xs: 12, sm: 6, md: 4 }} key={album.id}>
                            <Card sx={{ borderRadius: 3, boxShadow: 2, border: album.use ? '2px solid' : '1px solid transparent', borderColor: album.use ? 'primary.main' : 'divider' }}>
                                <CardMedia component="img" height="160" image={album.sampleFile ? getPhotoUrl(album.sampleFile, { width: 400, fit: "cover" }) : ""} />
                                <CardContent>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <Typography variant="h6">{album.title}</Typography>
                                        {user && (
                                            <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                                <Tooltip title="Synkroniser tidsluke/filer">
                                                    <IconButton
                                                        onClick={(e) => handleSync(album.id, e)}
                                                        disabled={syncingId === album.id}
                                                    >
                                                        <SyncIcon sx={{ animation: syncingId === album.id ? 'spin 1s linear infinite' : 'none', '@keyframes spin': { '0%': { transform: 'rotate(0deg)' }, '100%': { transform: 'rotate(360deg)' } } }} />
                                                    </IconButton>
                                                </Tooltip>
                                                <IconButton onClick={() => { setEditingAlbum(album); setOpenDialog(true); }}><EditIcon /></IconButton>
                                                <IconButton onClick={(e) => handleToggleActive(album, e)} color={album.use ? "primary" : "default"}>
                                                    {album.use ? <CheckCircleIcon /> : <RadioButtonUncheckedIcon />}
                                                </IconButton>
                                                <IconButton color="error" onClick={(e) => openDeleteDialog(album, e)}><DeleteOutlineIcon /></IconButton>
                                            </Box>
                                        )}
                                    </Box>
                                    <Typography variant="body2" color="text.secondary">{album.description}</Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            )}

            {user && (
                <Fab color="primary" sx={{ position: 'fixed', bottom: 24, right: 24 }} onClick={() => { setEditingAlbum(null); setOpenDialog(true); }}>
                    <AddIcon />
                </Fab>
            )}

            {user && (
                <AlbumDialog
                    open={openDialog}
                    onClose={() => setOpenDialog(false)}
                    onSave={handleSave}
                    editAlbum={editingAlbum}
                />
            )}

            {/* Slettebekreftelse Dialog */}
            <Dialog open={deleteModalOpen} onClose={() => setDeleteModalOpen(false)}>
                <DialogTitle>Slett album fra Kammich?</DialogTitle>
                <DialogContent>
                    <Typography variant="body1" sx={{ mb: 2 }}>
                        Er du sikker på at du vil slette <strong>{albumToDelete?.title}</strong>?
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Dette vil kun fjerne albumet fra Kammich. Albumet vil fremdeles ligge intakt på Immich, men Kammich vil ikke lenger kunne administrere det.
                    </Typography>
                </DialogContent>
                <DialogActions sx={{ p: 2, pt: 0 }}>
                    <Button onClick={() => setDeleteModalOpen(false)} color="inherit">
                        Avbryt
                    </Button>
                    <Button onClick={confirmDelete} variant="contained" color="error">
                        Slett
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}