import { useEffect, useState, useRef, useCallback, useMemo } from 'react';
import { Box, Typography, CircularProgress, keyframes, useTheme, useMediaQuery, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { useParams } from 'react-router-dom';
import type { RemoteFile } from "../types/types";
import { getPhotos, getPhotoThumbUrl, getPhotoUrl } from "../api/requests/photo";

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

    // Bruk referanser for å unngå at lukkede funksjoner (closures) henger igjen på gammel state
    const loadingRef = useRef(loading);
    loadingRef.current = loading;

    const hasMoreRef = useRef(hasMore);
    hasMoreRef.current = hasMore;

    const pageRef = useRef(page);
    pageRef.current = page;

    // Nullstill alt når 'sn' endrer seg
    useEffect(() => {
        setPhotos([]);
        setPage(0);
        setHasMore(true);

        let isMounted = true;
        setLoading(true);

        getPhotos(0, 30, sn)
            .then(res => {
                if (!isMounted) return;
                setPhotos(res.data);
                setHasMore(res.hasMore);
                setPage(1); // Klar for neste side
            })
            .catch(err => {
                if (isMounted) console.error("Kunne ikke laste første side:", err);
            })
            .finally(() => {
                if (isMounted) setLoading(false);
            });

        return () => {
            isMounted = false;
        };
    }, [sn]);

    // Stabil loadMore som bruker refs og aldri går i spinn
    const loadMore = useCallback(async () => {
        if (loadingRef.current || !hasMoreRef.current) return;

        setLoading(true);
        try {
            const currentPage = pageRef.current;
            const res = await getPhotos(currentPage, 30, sn);

            setPhotos(prev => {
                const existingIds = new Set(prev.map(p => p.id));
                const newPhotos = res.data.filter(p => !existingIds.has(p.id));
                return [...prev, ...newPhotos];
            });

            setHasMore(res.hasMore);
            setPage(p => p + 1);
        } catch (err) {
            console.error("Kunne ikke laste flere bilder:", err);
        } finally {
            setLoading(false);
        }
    }, [sn]);

    const lastElementRef = useCallback((node: HTMLDivElement | null) => {
        if (loadingRef.current) return;
        if (observerRef.current) observerRef.current.disconnect();

        observerRef.current = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting && hasMoreRef.current && !loadingRef.current) {
                loadMore();
            }
        });

        if (node) observerRef.current.observe(node);
    }, [loadMore]);

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

            <Box sx={{ display: 'flex', gap: { xs: '8px', sm: '12px' }, alignItems: 'flex-start' }}>
                {photoColumns.map((colPhotos, colIndex) => (
                    <Box key={colIndex} sx={{ display: 'flex', flexDirection: 'column', gap: { xs: '8px', sm: '12px' }, flex: 1 }}>
                        {colPhotos.map((photo) => (
                            <Box
                                key={photo.id}
                                onClick={() => setSelectedPhoto(photo)}
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

            {selectedPhoto && (
                <Box
                    onClick={() => setSelectedPhoto(null)}
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

                    <Box
                        component="img"
                        src={getPhotoUrl(selectedPhoto, { auto: "format" })}
                        alt={selectedPhoto.fileName}
                        onClick={(e) => e.stopPropagation()}
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