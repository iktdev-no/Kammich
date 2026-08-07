import {
    AppBar, Toolbar, IconButton, Typography, Box, useTheme, Avatar, Tooltip, keyframes
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import { useIsMobile } from "../hooks/useIsMobile";
import { useColorMode } from "../context/ColorModeContext";
import { DarkModeOutlined, LightModeOutlined } from "@mui/icons-material";
import NotificationPopover from "../components/NotificationPopover";
import SyncIcon from "../components/icons/SyncIcon";
import CancelIcon from "@mui/icons-material/Clear";



// 1. Definer keyframes for rotasjon
const spin = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
`;

export default function Header({ onToggleSidebar }: { onToggleSidebar: () => void }) {
    const isMobile = useIsMobile();
    const theme = useTheme();
    const { mode, toggleColorMode } = useColorMode();

    const headerHeight = isMobile ? theme.layout.headerMobile : theme.layout.headerDesktop;

    return (
        <AppBar position="fixed" elevation={0} sx={{
            backgroundColor: theme.palette.background.default,
            color: theme.palette.text.primary,
            borderBottom: `1px solid ${theme.palette.divider}`,
            height: `${headerHeight}px`,
            justifyContent: "center",
        }}>
            <Toolbar sx={{ minHeight: `${headerHeight}px !important`, px: 2 }}>
                {isMobile && (
                    <IconButton color="inherit" edge="start" onClick={onToggleSidebar} sx={{ mr: 2 }}>
                        <MenuIcon />
                    </IconButton>
                )}

                {/* Logo-wrapper */}
                <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center' }}>
                    <Typography variant="h6" sx={{ fontWeight: 600, letterSpacing: "-0.5px" }}>
                        Kammich
                    </Typography>
                </Box>

                <Box sx={{
                    background: theme.palette.background.paper,
                    p: 1,
                    borderRadius: 10,
                    display: "flex",
                    flexDirection: "row",
                    alignContent: "center",
                    alignItems: "center",
                    justifyContent: "space-between",
                    flexWrap: "nowrap",
                    minWidth: "150px",
                    mr: 2
                }}>
                    <Box sx={{
                        display: "flex",
                        pl: 1,
                        pr: 0.5,
                    }}>
                        <Typography>
                            Importing
                        </Typography>
                    </Box>
                    <Box sx={{
                        display: "flex",
                        position: "relative", // Viktig for å posisjonere Cancel over Sync
                        borderRadius: "50%",
                        bgcolor: theme.palette.primary.main,
                        p: 0.6,
                        ml: 1,
                        alignItems: "center",
                        justifyContent: "center",
                        cursor: "pointer", // Vis musepeker ved hover
                        // Vis cancel-ikon og endre farge ved hover på denne boksen:
                        "&:hover": {
                            bgcolor: theme.palette.error.main, // Bytter bakgrunn til rød ved hover
                            "& .sync-icon": {
                                animation: "none", // Stopper spinning ved hover (valgfritt)
                                opacity: 0,        // Skjuler sync-ikonet
                            },
                            "& .cancel-icon": {
                                opacity: 1,        // Viser cancel-ikonet
                            }
                        }
                    }}>
                        {/* Sync-ikonet som snurrer */}
                        <SyncIcon className="sync-icon" sx={{
                            animation: `${spin} 1.5s linear infinite`,
                            color: theme.palette.primary.contrastText,
                            transition: "opacity 0.2s ease",
                        }} />

                        {/* Cancel-ikonet som er skjult til man hovrer */}
                        <CancelIcon className="cancel-icon" sx={{
                            position: "absolute",
                            opacity: 0, // Usynlig som standard
                            color: theme.palette.error.contrastText,
                            transition: "opacity 0.2s ease",
                        }} />
                    </Box>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Tooltip title="Bytt tema">
                        <IconButton onClick={toggleColorMode} color="inherit">
                            {mode === 'dark' ? <DarkModeOutlined /> : <LightModeOutlined />}
                        </IconButton>
                    </Tooltip>

                    <NotificationPopover />

                    <Avatar sx={{ width: 32, height: 32, ml: 1, bgcolor: theme.palette.primary.main }}>
                        K
                    </Avatar>
                </Box>
            </Toolbar>
        </AppBar>
    );
}