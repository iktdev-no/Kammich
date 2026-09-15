import {
    AppBar, Toolbar, IconButton, Typography, Box, useTheme, Avatar, Tooltip,
    type SvgIconProps
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import { useIsMobile } from "../hooks/useIsMobile";
import { useColorMode } from "../context/ColorModeContext";
import { DarkModeOutlined, LightModeOutlined } from "@mui/icons-material";
import NotificationPopover from "../components/NotificationPopover";
import LoginIcon from '@mui/icons-material/Login';
import { ImportIndicator } from "./ImportIndicator";
import ImmichIcon from "../components/icons/ImmichIcon";
import { useNavigate } from "react-router-dom";
import { useSseSelector } from "../sse/useSseSelector";
import { KammichFav } from "../components/icons/KammichFav";
import { KammichIcon } from "../components/icons/Kammich";
import FilmRollIcon from "../components/icons/FilmRoll";
import AnimatedUploadIcon from "../components/icons/AnimatedUploadIcon";


export default function Header({ onToggleSidebar }: { onToggleSidebar: () => void }) {
    const isMobile = useIsMobile();
    const theme = useTheme();
    const { mode, toggleColorMode } = useColorMode();
    const navigate = useNavigate();
    const immichUser = useSseSelector(state => state.immichUserMe)

    const headerHeight = isMobile ? theme.layout.headerMobile : theme.layout.headerDesktop;


    const importDevices = useSseSelector(state => state.importDevices) || {};

    const isImporting = Object.values(importDevices).some(
        device => device.state !== "Completed" && device.state !== "Canceled"
    );

    const activeUploadProgress = useSseSelector(
        state => state.activeUploadProgress
    );

    const isUploading = Object.values(activeUploadProgress).some(
        upload => upload.state === "Running"
    );


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
                <Box sx={{ flexGrow: 1, display: "flex", alignItems: "center", gap: 1 }}>
                    <KammichIcon sx={{ fontSize: 52 }} />
                    <Box sx={{ display: "flex", alignItems: "center", height: 52, marginTop: 0.5 }}>
                        <Typography
                            variant="h6"
                            sx={{ fontWeight: 600, letterSpacing: "-0.5px" }}
                        >
                            Kammich
                        </Typography>
                    </Box>
                </Box>

                {isUploading && (
                    <IconButton onClick={() => navigate("/upload")}>
                        <AnimatedUploadIcon fontSize="medium" accentColor={theme.palette.primary.main} />
                    </IconButton>
                )}

                {isImporting && (
                    <IconButton onClick={() => navigate("/import")}>
                        <FilmRollIcon fontSize="medium" />
                    </IconButton>
                )}


                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Tooltip title="Bytt tema">
                        <IconButton onClick={toggleColorMode} color="inherit">
                            {mode === 'dark' ? <DarkModeOutlined /> : <LightModeOutlined />}
                        </IconButton>
                    </Tooltip>

                    <NotificationPopover />

                    <Box sx={{ display: "flex", flexDirection: "row", flexWrap: "nowrap", maxHeight: `${headerHeight}px`, alignContent: "center", alignItems: "center", ml: 1 }}>
                        {!immichUser ? (
                            <IconButton onClick={() => navigate("/settings/immich")}>
                                <ImmichLoginBadge />
                            </IconButton>
                        ) : (
                            <Avatar
                                src={`/api/v1/immich/profile-image?userId=${immichUser.id}`}
                                sx={{ width: 32, height: 32, cursor: "pointer" }}
                                onClick={() => navigate(`/settings/immich/me`)}
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