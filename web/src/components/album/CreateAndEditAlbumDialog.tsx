import { useState, useEffect } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, TextField, Button, Box } from "@mui/material";
import type { Album } from "../../types/types";

interface AlbumDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (data: any) => Promise<void>;
    editAlbum?: Album | null;
}

export function AlbumDialog({ open, onClose, onSave, editAlbum }: AlbumDialogProps) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");

    useEffect(() => {
        if (editAlbum) {
            setTitle(editAlbum.title);
            setDescription(editAlbum.description || "");
            setStartDate(editAlbum.startDate || "");
            setEndDate(editAlbum.endDate || "");
        } else {
            setTitle("");
            setDescription("");
            setStartDate("");
            setEndDate("");
        }
    }, [editAlbum, open]);

    const handleSave = async () => {
        if (!title.trim()) return;
        await onSave({
            albumName: title,
            description: description || null,
            startDate: startDate || null,
            endDate: endDate || null,
        });
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="xs"
            fullWidth
            slotProps={{
                paper: {
                    sx: {
                        borderRadius: 4,
                        p: 1.5,
                        backgroundImage: 'none'
                    }
                }
            }}
        >
            <DialogTitle sx={{ fontWeight: 700, pb: 1, fontSize: '1.25rem' }}>
                {editAlbum ? "Rediger album" : "Nytt album"}
            </DialogTitle>

            <DialogContent sx={{ pt: '10px !important' }}>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, mt: 1 }}>
                    <TextField
                        label="Tittel"
                        fullWidth
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                    />
                    <TextField
                        label="Beskrivelse"
                        fullWidth
                        multiline
                        rows={3}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                    />
                    <TextField
                        label="Startdato"
                        type="date"
                        slotProps={{ inputLabel: { shrink: true } }}
                        fullWidth
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                    />
                    <TextField
                        label="Sluttdato"
                        type="date"
                        slotProps={{ inputLabel: { shrink: true } }}
                        fullWidth
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                    />
                </Box>
            </DialogContent>

            <DialogActions sx={{ p: 2, px: 3, pb: 2 }}>
                <Button
                    onClick={onClose}
                    color="inherit"
                    sx={{ textTransform: 'none', color: 'text.secondary' }}
                >
                    Avbryt
                </Button>
                <Button
                    onClick={handleSave}
                    variant="contained"
                    disabled={!title.trim()}
                    sx={{ textTransform: 'none', borderRadius: 2, px: 3 }}
                >
                    {editAlbum ? "Lagre endringer" : "Opprett"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}