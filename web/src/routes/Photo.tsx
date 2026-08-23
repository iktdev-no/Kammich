import { useEffect, useState, useRef, useCallback, useMemo, type MouseEvent } from 'react';
import { Box, Typography, CircularProgress, keyframes, useTheme, useMediaQuery, IconButton, Menu, MenuItem, ListItemIcon, ListItemText } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import { useParams } from 'react-router-dom';
import type { RemoteFile } from "../types/types";
import { getPhotos, getPhotoThumbUrl, getPhotoUrl } from "../api/requests/photo";
import { uploadFile } from "../api/requests/upload";
import { useSseSelector } from '../sse/useSseSelector';

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

    // State for Context Menu
    const [contextMenu, setContextMenu] = useState<{ mouseX: number; mouseY: number } | null>(null);
    const [contextPhoto, setContextPhoto] = useState<RemoteFile | null>(null);

    // Hent ut bruker fra SSE kontekst
    const immichUser = useSseSelector(state => state.immichUserMe);
    const userId = immichUser?.id;

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

    const loadingRef = useRef(loading);
    loadingRef.current = loading;

    const hasMoreRef = useRef(hasMore);
    hasMoreRef.current = hasMore;

    const pageRef = useRef(page);
    pageRef.current = page;

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
                setPage(1);
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

    // Håndter høyreklikk på bilde (kun aktivt hvis brukeren er logget inn)
    const handleContextMenu = (event: MouseEvent<HTMLDivElement>, photo: RemoteFile) => {
        if (!userId) return; // Ikke gjør noe eller åpne meny om brukeren ikke er logget inn
        event.preventDefault();
        setContextPhoto(photo);
        setContextMenu(
            contextMenu === null
                ? { mouseX: event.clientX + 2, mouseY: event.clientY - 6 }
                : null,
        );
    };

    const handleCloseContextMenu = () => {
        setContextMenu(null);
        setContextPhoto(null);
    };

    // Trigget når bruker trykker på "Last opp" i menyen
    const handleUploadClick = async () => {
        if (!userId || !contextPhoto) {
            console.error("Mangler bruker-ID eller bilde-ID for opplasting");
            handleCloseContextMenu();
            return;
        }

        try {
            await uploadFile(userId, contextPhoto.id);
            console.log(`Startet opplasting for fil ${contextPhoto.id}`);
        } catch (err) {
            console.error("Feil ved opplasting av fil:", err);
        } finally {
            handleCloseContextMenu();
        }
    };

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
                                onContextMenu={(e) => handleContextMenu(e, photo)}
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

            {/* Context Menu for bilder (vises kun om brukeren erlogget inn) */}
            {userId && (
                <Menu
                    open={contextMenu !== null}
                    onClose={handleCloseContextMenu}
                    anchorReference="anchorPosition"
                    anchorPosition={
                        contextMenu !== null
                            ? { top: contextMenu.mouseY, left: contextMenu.mouseX }
                            : undefined
                    }
                >
                    <MenuItem onClick={handleUploadClick}>
                        <ListItemIcon>
                            <CloudUploadIcon fontSize="small" />
                        </ListItemIcon>
                        <ListItemText>Last opp bilde</ListItemText>
                    </MenuItem>
                </Menu>
            )}

            {/* Fullskjerm-visning */}
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