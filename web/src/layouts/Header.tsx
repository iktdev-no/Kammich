import {
    AppBar, Toolbar, IconButton, Typography, Box, useTheme, Avatar, Tooltip, keyframes,
    type SvgIconProps
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import { useIsMobile } from "../hooks/useIsMobile";
import { useColorMode } from "../context/ColorModeContext";
import { DarkModeOutlined, LightModeOutlined } from "@mui/icons-material";
import NotificationPopover from "../components/NotificationPopover";
import SyncIcon from "../components/icons/SyncIcon";
import CancelIcon from "@mui/icons-material/Clear";
import LoginIcon from '@mui/icons-material/Login';
import { ImportIndicator } from "./ImportIndicator";
import ImmichIcon from "../components/icons/ImmichIcon";
import { useNavigate } from "react-router-dom";
import { useSseSelector } from "../sse/useSseSelector";


export default function Header({ onToggleSidebar }: { onToggleSidebar: () => void }) {
    const isMobile = useIsMobile();
    const theme = useTheme();
    const { mode, toggleColorMode } = useColorMode();
    const naviage = useNavigate();
    const immichUser = useSseSelector(state => state.immichUserMe)
    const immichAuth = useSseSelector(state => state.immichApiKeyInUse)

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

                <ImportIndicator />


                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Tooltip title="Bytt tema">
                        <IconButton onClick={toggleColorMode} color="inherit">
                            {mode === 'dark' ? <DarkModeOutlined /> : <LightModeOutlined />}
                        </IconButton>
                    </Tooltip>

                    <NotificationPopover />

                    <Box sx={{ display: "flex", flexDirection: "row", flexWrap: "nowrap", maxHeight: `${headerHeight}px`, alignContent: "center", alignItems: "center", ml: 1 }}>
                        {!immichUser ? (
                            <IconButton onClick={() => naviage("/settings/immich/login")}>
                                <ImmichLoginBadge />
                            </IconButton>
                        ) : (
                            <Avatar
                                src={`/api/v1/immich/profile-image?userId=${immichUser.id}`}
                                sx={{ width: 32, height: 32 }}
                            >
                                {immichUser.name?.[0]}
                            </Avatar>
                        )}

                    </Box>



                </Box>
            </Toolbar>
        </AppBar>
    );
}


export function ImmichLoginBadge({ sx, ...props }: SvgIconProps) {
    return (
        <Box
            component="span"
            sx={{
                position: "relative",
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                verticalAlign: "middle",
            }}
        >
            {/* Hovedikonet */}
            <ImmichIcon
                sx={{ height: 32, width: 32, ...sx }}
                {...props}
            />

            {/* Login overlay-ikon */}
            <Box
                sx={{
                    position: "absolute",
                    bottom: -2,
                    right: -4,
                    bgcolor: "#111111",
                    borderRadius: "50%",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    p: "2px",
                    border: "1px solid #333333",
                }}
            >
                <LoginIcon
                    sx={{
                        fontSize: 14,
                        color: "white"
                    }}
                />
            </Box>
        </Box>
    );
}