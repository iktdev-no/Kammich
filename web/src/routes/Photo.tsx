import { useEffect, useState } from 'react';
import { ImageList, ImageListItem, Box, Typography, Button, CircularProgress } from '@mui/material';
import type { RemoteFile } from "../types/types";
import { getPhotos, getPhotoThumbUrl, getPhotoUrl } from "../api/photo";

export default function Photo() {
    const [photos, setPhotos] = useState<RemoteFile[]>([]);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [page, setPage] = useState(0);

    const loadMore = async () => {
        if (loading || !hasMore) return;

        setLoading(true);
        try {
            const res = await getPhotos(page, 30);

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

    // Last inn første side når komponenten mountes (fungerer knirkefritt med Strict Mode)
    useEffect(() => {
        loadMore();
    }, []);

    return (
        <Box sx={{ p: 2, bgcolor: 'background.default', minHeight: '100vh' }}>
            <Typography variant="h4" sx={{ mb: 3, color: 'text.primary' }}>Bibliotek</Typography>

            <ImageList variant="masonry" cols={6} gap={8}>
                {photos.map((photo, index) => {
                    // De første 6 bildene er synlige med en gang ("above the fold")
                    const isAboveTheFold = index < 6;

                    return (
                        <ImageListItem key={photo.id} sx={{ borderRadius: 2, overflow: 'hidden' }}>
                            <img
                                // Sender med width=300 for å matche rutenettet og unngå store filer
                                src={getPhotoThumbUrl(photo, { width: 300, fit: "crop", auto: "format" })}
                                alt={photo.fileName}
                                loading={isAboveTheFold ? "eager" : "lazy"}
                                fetchPriority={isAboveTheFold ? "high" : "auto"}
                                decoding={isAboveTheFold ? "sync" : "async"}
                                style={{
                                    transition: 'transform 0.2s',
                                    display: 'block',
                                    width: '100%',
                                    height: 'auto'
                                }}
                                onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
                                onMouseOut={(e) => e.currentTarget.style.transform = 'scale(1)'}
                            />
                        </ImageListItem>
                    );
                })}
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