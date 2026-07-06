import { 
    AppBar, Toolbar, IconButton, Typography, Box, useTheme, Avatar, Tooltip 
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import { useIsMobile } from "../hooks/useIsMobile";
import { useColorMode } from "../context/ColorModeContext";
import { DarkModeOutlined, LightModeOutlined } from "@mui/icons-material";
import NotificationPopover from "../components/NotificationPopover";

// Importer popover-komponenten

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

                {/* Logo-wrapper: Enkel å bytte ut med SVG senere */}
                <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center' }}>
                    <Typography variant="h6" sx={{ fontWeight: 600, letterSpacing: "-0.5px" }}>
                        Kammich
                    </Typography>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Tooltip title="Bytt tema">
                        <IconButton onClick={toggleColorMode} color="inherit">
                            {mode === 'dark' ? <DarkModeOutlined /> : <LightModeOutlined />}
                        </IconButton>
                    </Tooltip>

                    {/* Her er den nye popoveren attached */}
                    <NotificationPopover />

                    <Avatar sx={{ width: 32, height: 32, ml: 1, bgcolor: theme.palette.primary.main }}>
                        K
                    </Avatar>
                </Box>
            </Toolbar>
        </AppBar>
    );
}