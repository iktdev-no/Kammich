import { useEffect, useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, TextField, Button, Box } from "@mui/material";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDateFns } from "@mui/x-date-pickers/AdapterDateFns";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import type { Album } from "../../types/types";
import { nb } from 'date-fns/locale';

interface AlbumDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (data: any) => Promise<void>;
    editAlbum?: Album | null;
}

// Hjelpefunksjon for å parse dato-streng til Date-objekt
const parseISOToDate = (dateString?: string | null): Date | null => {
    if (!dateString) return null;
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? null : date;
};

// Formater Date-objekt til ren "YYYY-MM-DD"-streng uten tidssone-tull
const formatDateOnly = (dateObj: Date | null): string | null => {
    if (!dateObj || isNaN(dateObj.getTime())) return null;

    try {
        const year = dateObj.getFullYear();
        const month = String(dateObj.getMonth() + 1).padStart(2, '0');
        const day = String(dateObj.getDate()).padStart(2, '0');

        return `${year}-${month}-${day}`;
    } catch {
        return null;
    }
};

export function AlbumDialog({ open, onClose, onSave, editAlbum }: AlbumDialogProps) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");

    const [startDateObj, setStartDateObj] = useState<Date | null>(null);
    const [endDateObj, setEndDateObj] = useState<Date | null>(null);

    useEffect(() => {
        if (editAlbum) {
            setTitle(editAlbum.title);
            setDescription(editAlbum.description || "");

            setStartDateObj(parseISOToDate(editAlbum.startDate));
            setEndDateObj(parseISOToDate(editAlbum.endDate));
        } else {
            setTitle("");
            setDescription("");
            setStartDateObj(null);
            setEndDateObj(null);
        }
    }, [editAlbum, open]);

    // Automatisk sett sluttdato til å matche startdato hvis den er tom
    const handleStartDateChange = (newDate: Date | null) => {
        setStartDateObj(newDate);
        if (newDate && !endDateObj) {
            setEndDateObj(newDate);
        }
    };

    const handleSave = async () => {
        if (!title.trim()) return;
        await onSave({
            albumName: title,
            description: description || null,
            startDate: formatDateOnly(startDateObj),
            endDate: formatDateOnly(endDateObj),
        });
    };

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={nb}>
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

                        {/* Startdato */}
                        <DatePicker
                            label="Startdato"
                            value={startDateObj}
                            onChange={(newValue) => handleStartDateChange(newValue)}
                            slotProps={{ textField: { fullWidth: true } }}
                        />

                        {/* Sluttdato */}
                        <DatePicker
                            label="Sluttdato"
                            value={endDateObj}
                            onChange={(newValue) => setEndDateObj(newValue)}
                            slotProps={{ textField: { fullWidth: true } }}
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
        </LocalizationProvider>
    );
}