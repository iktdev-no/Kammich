import { useEffect, useState, useRef, useCallback, useMemo } from 'react';
import { Box, Typography, CircularProgress, keyframes, useTheme, useMediaQuery, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { useParams } from 'react-router-dom';
import type { RemoteFile } from "../types/types";
import { getPhotos, getPhotoThumbUrl, getPhotoUrl } from "../api/photo";

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
`;

export default function Photo() {
    const { sn } = useParams<{ sn: string }>();
    const [photos, setPhotos] = useState<RemoteFile[]>([]);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [page, setPage] = useState(0);

    // State for fullskjerm-visning av valgt bilde
    const [selectedPhoto, setSelectedPhoto] = useState<RemoteFile | null>(null);

    const theme = useTheme();
    const isXl = useMediaQuery(theme.breakpoints.up('xl'));
    const isLg = useMediaQuery(theme.breakpoints.up('lg'));
    const isMd = useMediaQuery(theme.breakpoints.up('md'));
    const isSm = useMediaQuery(theme.breakpoints.up('sm'));

    const numCols = useMemo(() => {
        if (isXl) return 6;
        if (isLg) return 5;
        if (isMd) return 4;
        if (isSm) return 3;
        return 2;
    }, [isXl, isLg, isMd, isSm]);

    const observerRef = useRef<IntersectionObserver | null>(null);

    useEffect(() => {
        setPhotos([]);
        setPage(0);
        setHasMore(true);
    }, [sn]);

    const loadMore = useCallback(async () => {
        if (loading || !hasMore) return;

        setLoading(true);
        try {
            const res = await getPhotos(page, 30, sn);

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
    }, [page, hasMore, loading, sn]);

    useEffect(() => {
        if (page === 0 && photos.length === 0 && hasMore) {
            loadMore();
        }
    }, [page, photos.length, hasMore, loadMore]);

    const lastElementRef = useCallback((node: HTMLDivElement | null) => {
        if (loading) return;
        if (observerRef.current) observerRef.current.disconnect();

        observerRef.current = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting && hasMore) {
                loadMore();
            }
        });

        if (node) observerRef.current.observe(node);
    }, [loading, hasMore, loadMore]);

    const photoColumns = useMemo(() => {
        const cols: RemoteFile[][] = Array.from({ length: numCols }, () => []);
        photos.forEach((photo, index) => {
            cols[index % numCols].push(photo);
        });
        return cols;
    }, [photos, numCols]);

    return (
        <Box sx={{ p: { xs: 1.5, sm: 3 }, bgcolor: 'background.default', minHeight: '100vh', position: 'relative' }}>
            <Typography variant="h4" sx={{ mb: 4, fontWeight: 600, color: 'text.primary' }}>
                {sn ? `Bibliotek (${sn})` : "Bibliotek"}
            </Typography>

            {/* Kolonner med bilder */}
            <Box sx={{ display: 'flex', gap: { xs: '8px', sm: '12px' }, alignItems: 'flex-start' }}>
                {photoColumns.map((colPhotos, colIndex) => (
                    <Box key={colIndex} sx={{ display: 'flex', flexDirection: 'column', gap: { xs: '8px', sm: '12px' }, flex: 1 }}>
                        {colPhotos.map((photo) => (
                            <Box
                                key={photo.id}
                                onClick={() => setSelectedPhoto(photo)} // Åpne i fullskjerm ved klikk
                                sx={{
                                    borderRadius: 2,
                                    overflow: 'hidden',
                                    bgcolor: 'background.paper',
                                    boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
                                    cursor: 'pointer',
                                    animation: `${fadeIn} 0.3s ease-out forwards`,
                                    '&:hover img': {
                                        transform: 'scale(1.04)',
                                    },
                                }}
                            >
                                <img
                                    src={getPhotoThumbUrl(photo, { width: 400, auto: "format" })}
                                    alt={photo.fileName}
                                    loading="lazy"
                                    decoding="async"
                                    style={{
                                        display: 'block',
                                        width: '100%',
                                        height: 'auto',
                                        transition: 'transform 0.25s ease',
                                        willChange: "transform"
                                    }}
                                />
                            </Box>
                        ))}
                    </Box>
                ))}
            </Box>

            <div ref={lastElementRef} style={{ height: '20px', margin: '40px 0' }} />

            {loading && (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                    <CircularProgress size={36} thickness={4} />
                </Box>
            )}

            {/* 🖥️ FULLSKJERM / LIGHTBOX MODAL */}
            {selectedPhoto && (
                <Box
                    onClick={() => setSelectedPhoto(null)} // Lukk hvis man klikker utenfor bildet
                    sx={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        width: '100vw',
                        height: '100vh',
                        bgcolor: 'rgba(0, 0, 0, 0.95)',
                        zIndex: 9999,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        p: { xs: 2, sm: 4 },
                        animation: `${fadeIn} 0.2s ease-out forwards`,
                    }}
                >
                    {/* Lukk-knapp øverst i høyre hjørne */}
                    <IconButton
                        onClick={() => setSelectedPhoto(null)}
                        sx={{
                            position: 'absolute',
                            top: 20,
                            right: 20,
                            color: 'white',
                            bgcolor: 'rgba(255, 255, 255, 0.1)',
                            '&:hover': { bgcolor: 'rgba(255, 255, 255, 0.2)' },
                            zIndex: 10000,
                        }}
                    >
                        <CloseIcon fontSize="large" />
                    </IconButton>

                    {/* Fullskjerm-bilde med ekte high-res source */}
                    <Box
                        component="img"
                        src={getPhotoUrl(selectedPhoto, { auto: "format" })}
                        alt={selectedPhoto.fileName}
                        onClick={(e) => e.stopPropagation()} // Hindre at klikk på selve bildet lukker modalen
                        sx={{
                            maxWidth: '100%',
                            maxHeight: '100%',
                            objectFit: 'contain',
                            borderRadius: 1,
                            boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
                        }}
                    />
                </Box>
            )}
        </Box>
    );
}