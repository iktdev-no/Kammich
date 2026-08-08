import { useState } from "react";
import { Box, Typography, Menu, MenuItem, IconButton, useTheme, keyframes } from "@mui/material";
import SyncIcon from "../components/icons/SyncIcon";
import CancelIcon from "@mui/icons-material/Clear";

// Animasjon for spinning
const spin = keyframes`
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
`;

interface ActiveImport {
    deviceId: string;
    deviceName: string;
    index: number;
    total: number;
}

export default function ImportHeaderIndicator({ activeImports }: { activeImports: ActiveImport[] }) {
    const theme = useTheme();
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const open = Boolean(anchorEl);

    // Hvis det ikke er noen aktive importer, ikke vis noe
    if (activeImports.length === 0) return null;

    const handleMouseEnter = (event: React.MouseEvent<HTMLElement>) => {
        // Vis dropdown på hover KUN hvis det er mer enn 1 enhet
        if (activeImports.length > 1) {
            setAnchorEl(event.currentTarget);
        }
    };

    const handleMouseLeave = () => {
        setAnchorEl(null);
    };

    const handleClickSingle = (deviceId: string) => {
        // Hvis det bare er 1 enhet, betyr klikk at vi avbryter den direkte
        if (activeImports.length === 1) {
            cancelImport(deviceId);
        }
    };

    const cancelImport = (deviceId: string) => {
        console.log(`Kansellerer import for: ${deviceId}`);
        // Legg til ditt API-kall her
        setAnchorEl(null);
    };

    return (
        <Box
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            sx={{
                display: "flex",
                flexDirection: "row",
                justifyContent: "flex-end",
                alignContent: "center",
                flexWrap: "nowrap",
                alignItems: "center",
                gap: 1,
                background: theme.palette.background.paper,
                p: 0.5,
                borderRadius: 10,
                cursor: "pointer",
                mr: 2,
            }}
        >
            <Box sx={{
                display: "flex",
                pl: 1
            }}>
                {activeImports.length === 1 ? (
                    <>
                        <Typography>
                            Importing {activeImports[0].index} of {activeImports[0].total} from {activeImports[0].deviceName}
                        </Typography>
                    </>
                ) : (
                    <Typography variant="body2">
                        {activeImports.length === 1 ? activeImports[0].deviceName : `${activeImports.length} importerer`}
                    </Typography>
                )}
            </Box>

            {/* Selve sirkelen med ikonet */}
            <Box
                onClick={() => activeImports.length === 1 && handleClickSingle(activeImports[0].deviceId)}
                sx={{
                    display: "flex",
                    position: "relative",
                    borderRadius: "50%",
                    bgcolor: theme.palette.primary.main,
                    p: 0.6,
                    alignItems: "center",
                    justifyContent: "center",
                    "&:hover": {
                        bgcolor: activeImports.length === 1 ? theme.palette.error.main : theme.palette.primary.main,
                        "& .sync-icon": {
                            animation: activeImports.length === 1 ? "none" : `${spin} 1.5s linear infinite`,
                            opacity: activeImports.length === 1 ? 0 : 1,
                        },
                        "& .cancel-icon": {
                            opacity: activeImports.length === 1 ? 1 : 0,
                        }
                    }
                }}
            >
                {/* Sync-ikon */}
                <SyncIcon className="sync-icon" sx={{
                    animation: `${spin} 1.5s linear infinite`,
                    color: theme.palette.primary.contrastText,
                    transition: "opacity 0.2s ease",
                }} />

                {/* Cancel-ikon vises kun ved hover hvis det er 1 enhet */}
                {activeImports.length === 1 && (
                    <CancelIcon className="cancel-icon" sx={{
                        position: "absolute",
                        opacity: 0,
                        color: theme.palette.error.contrastText,
                        fontSize: 18,
                        transition: "opacity 0.2s ease",
                    }} />
                )}
            </Box>

            {/* Dropdown-meny */}
            <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={() => setAnchorEl(null)}
                // Bruk slotProps i stedet for MenuListProps i nyere MUI
                slotProps={{
                    list: {
                        onMouseLeave: () => setAnchorEl(null),
                    },
                }}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            >
                <Typography variant="caption" sx={{ px: 2, py: 0.5, color: 'text.secondary', fontWeight: 'bold', display: 'block' }}>
                    Aktive importer:
                </Typography>
                {activeImports.map((item) => (
                    <MenuItem key={item.deviceId} sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
                        <Typography variant="body2">{item.deviceName}</Typography>
                        <IconButton
                            size="small"
                            color="error"
                            onClick={(e) => {
                                e.stopPropagation();
                                cancelImport(item.deviceId);
                            }}
                        >
                            <CancelIcon fontSize="small" />
                        </IconButton>
                    </MenuItem>
                ))}
            </Menu>
        </Box>
    );
}