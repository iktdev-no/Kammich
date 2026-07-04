import { AppBar, Toolbar, IconButton, Typography, Box, useTheme } from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import { useIsMobile } from "../hooks/useIsMobile";

export default function Header({ onToggleSidebar }: { onToggleSidebar: () => void }) {
    const isMobile = useIsMobile();
    const theme = useTheme();

  const headerHeight = isMobile
  ? theme.layout.headerMobile
  : theme.layout.headerDesktop;

    return (
        <AppBar
            position="fixed"
            elevation={0}
            sx={{
                backgroundColor: theme.palette.background.default,
                borderBottom: "1px solid #333",
                height: `${headerHeight}px`,
                justifyContent: "center",
            }}
        >
            <Toolbar sx={{ minHeight: `${headerHeight}px, !important` }}>
                {isMobile && (
                    <IconButton
                        color="inherit"
                        edge="start"
                        onClick={onToggleSidebar}
                        sx={{ mr: 2 }}
                    >
                        <MenuIcon />
                    </IconButton>
                )}

                <Typography variant="h6" sx={{ flexGrow: 1 }}>
                    Kammich
                </Typography>

                <Box>{/* Avatar / user menu */}</Box>
            </Toolbar>
        </AppBar>
    );
}
