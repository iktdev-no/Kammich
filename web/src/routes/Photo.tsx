import React, { useEffect, useState, useRef } from 'react';
import { ImageList, ImageListItem, Box, Typography, Button, CircularProgress } from '@mui/material';
import type { RemoteFile } from "../types/types";
import { getPhotos } from "../api/photo";

export default function Photo() {
    const [photos, setPhotos] = useState<RemoteFile[]>([]);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [page, setPage] = useState(0);
    
    // Bruk en ref for å hindre dobbel-kall i Strict Mode
    const isInitialLoad = useRef(true);

    useEffect(() => {
        if (isInitialLoad.current) {
            isInitialLoad.current = false;
            loadMore();
        }
    }, []); // Kjør kun én gang

    const loadMore = async () => {
        if (loading || !hasMore) return;
        
        setLoading(true);
        try {
            const res = await getPhotos(page, 30);
            
            // Unngå duplikater ved å filtrere på ID
            setPhotos(prev => {
                const existingIds = new Set(prev.map(p => p.id));
                const newPhotos = res.data.filter(p => !existingIds.has(p.id));
                return [...prev, ...newPhotos];
            });
            
            setHasMore(res.hasMore);
            setPage(p => p + 1);
        } catch (err) {
            console.error("Kunne ikke laste bilder:", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ p: 2, bgcolor: 'background.default', minHeight: '100vh' }}>
            <Typography variant="h4" sx={{ mb: 3, color: 'text.primary' }}>Bibliotek</Typography>

            <ImageList variant="masonry" cols={6} gap={8}>
                {photos.map((photo) => (
                    // Bruk photo.id som er unik fra DB
                    <ImageListItem key={photo.id} sx={{ borderRadius: 2, overflow: 'hidden' }}>
                        <img
                            src={`/api/photo/${photo.deviceId}/${photo.fileName}?w=248&fit=crop&auto=format`}
                            alt={photo.fileName}
                            loading="lazy"
                            style={{ transition: 'transform 0.2s' }}
                            onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
                            onMouseOut={(e) => e.currentTarget.style.transform = 'scale(1)'}
                        />
                    </ImageListItem>
                ))}
            </ImageList>

            {hasMore && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                    <Button variant="contained" onClick={loadMore} disabled={loading}>
                        {loading ? <CircularProgress size={24} /> : "Last inn flere"}
                    </Button>
                </Box>
            )}
        </Box>
    );
}