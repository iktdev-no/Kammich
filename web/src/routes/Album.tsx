import { useState, useEffect } from "react";
import {
    Box, Typography, Fab, Card, CardMedia, CardContent,
    Dialog, DialogTitle, DialogContent, DialogActions,
    TextField, Button, IconButton, Chip
} from "@mui/material";
import Grid from "@mui/material/Grid";
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import PhotoAlbumIcon from '@mui/icons-material/PhotoAlbum';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { getAlbums, createAlbum, updateAlbum, deleteAlbum } from "../api/requests/album";
import type { Album } from "../types/types";
import { getPhotoUrl } from "../api/requests/photo";
import { toast } from "react-toastify";
import { AlbumDialog } from "../components/album/CreateAndEditAlbumDialog";
import { useSseSelector } from "../sse/useSseSelector";

// --- HOVEDKOMPONENT ---
export function Album() {
    const user = useSseSelector(state => state.immichUserMe)
    const [albums, setAlbums] = useState<Album[]>([]);
    const [openDialog, setOpenDialog] = useState(false);
    const [editingAlbum, setEditingAlbum] = useState<Album | null>(null);

    const fetchAlbums = async () => {
        try {
            const data = await getAlbums();
            setAlbums(data);
        } catch (err) {
            console.error("Klarte ikke å hente album:", err);
        }
    };

    useEffect(() => { fetchAlbums(); }, []);

    const handleSave = async (formData: any) => {
        if (!user) return;
        try {
            if (editingAlbum) {
                await updateAlbum(editingAlbum.id, { ...formData, use: editingAlbum.use });
                toast.success("Album oppdatert!");
            } else {
                await createAlbum(formData);
                toast.success("Album opprettet!");
            }
            setOpenDialog(false);
            setEditingAlbum(null);
            fetchAlbums();
        } catch (err) {
            console.error("Feil ved lagring:", err);
        }
    };

    const handleDelete = async (id: number, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!user) return;
        if (!confirm("Er du sikker på at du vil slette dette albumet?")) return;
        try {
            await deleteAlbum(id);
            toast.success("Album slettet");
            fetchAlbums();
        } catch (err) {
            console.error("Feil ved sletting:", err);
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

    return (
        <Box sx={{ p: 3, position: 'relative', minHeight: '80vh' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 600 }}>Albums</Typography>

            {albums.length === 0 ? (
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
                                            <Box>
                                                <IconButton onClick={() => { setEditingAlbum(album); setOpenDialog(true); }}><EditIcon /></IconButton>
                                                <IconButton onClick={(e) => handleToggleActive(album, e)} color={album.use ? "primary" : "default"}>
                                                    {album.use ? <CheckCircleIcon /> : <RadioButtonUncheckedIcon />}
                                                </IconButton>
                                                <IconButton color="error" onClick={(e) => handleDelete(album.id, e)}><DeleteOutlineIcon /></IconButton>
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
        </Box>
    );
}