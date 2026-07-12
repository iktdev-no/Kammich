import { Box, List, useTheme } from "@mui/material";
import { useLocation, useNavigate } from "react-router-dom";
import { sidebarStyles } from "../../theme/sidebarTheme";
import { CacheIndicator } from "./CacheIndicator";
import { StatusIndicator } from "./StatusIndicator";
import { SidebarItemRenderer } from "./SidebarItemRendrer";
import PhotoLibraryOutlinedIcon from "@mui/icons-material/PhotoLibraryOutlined";
import CloudUploadOutlinedIcon from "@mui/icons-material/CloudUploadOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import SettingsIcon from "@mui/icons-material/Settings";
import CameraAltOutlinedIcon from '@mui/icons-material/CameraAltOutlined';
import PermMediaOutlinedIcon from '@mui/icons-material/PermMediaOutlined';
import KeyIcon from '@mui/icons-material/Key';
import CameraIcon from '@mui/icons-material/Camera';
import WifiIcon from '@mui/icons-material/Wifi';
import WifiTetheringIcon from '@mui/icons-material/WifiTethering';

import SdStorageOutlinedIcon from '@mui/icons-material/SdStorageOutlined';
import CableIcon from '@mui/icons-material/Cable';
import type { SidebarItem } from "./SidebarItemTypes";
import { useSseSelector } from "../../sse/useSseSelector";
import { useEffect, useMemo } from "react";
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';

export interface SidebarMenuProps {
    width: number;
    onItemClick?: () => void;
}

export default function SidebarMenu({ width, onItemClick }: SidebarMenuProps) {
    const theme = useTheme();
    const sx = sidebarStyles(theme);
    const navigate = useNavigate();
    const connectionStatus = useSseSelector(state => state.connectionStatus);
    const devices = useSseSelector(state => state.devices);
    const location = useLocation();
    const isSettings = location.pathname.startsWith("/settings");


    // Bruk useMemo slik at menyen oppdateres kun når 'devices' endres
    const mainMenuItems: SidebarItem[] = useMemo(() => [
        {
            label: "Photos",
            icon: PhotoLibraryOutlinedIcon,
            to: "/",
            children: devices.map(d => ({
                label: d.model ?? d.name,
                icon: (d.type !== "BLOCK") ? CameraAltOutlinedIcon : SdStorageOutlinedIcon,
                to: `/photo/${d.id}`,
            })),
        },
        {
            label: "Devices",
            icon: CableIcon,
            to: "/devices",
            children: devices.map(d => ({
                label: d.model ?? d.name,
                icon: (d.type !== "BLOCK") ? CameraAltOutlinedIcon : SdStorageOutlinedIcon,
                to: `/camera/${d.id}`,
            })),
        },
        { label: "Upload", icon: CloudUploadOutlinedIcon, to: "/upload" },
        {
            label: "Settings",
            icon: SettingsIcon,
            to: "/settings",
            sx: { marginTop: "auto" }
        },

    ], [devices]); // <-- Dependency: Re-kalkulerer kun når devices endres


    const settingsMenuItems: SidebarItem[] = useMemo(() => [
        {
            label: "Back",
            icon: ArrowBackRoundedIcon,
            to: "/",
        },
        {
            label: "Settings",
            icon: SettingsIcon,
            to: "/settings",
        },
        {
            label: "Wifi",
            icon: WifiIcon,
            children: [
                {
                    label: "Wifi",
                    icon: WifiIcon,
                    to: "/settings/wifi"
                },
                {
                    label: "Direct",
                    icon: WifiTetheringIcon,
                    to: "/settings/wificonnect"
                }
            ]
        },
    ], [])

    const activeItems = isSettings ? settingsMenuItems : mainMenuItems;

    return (
        <Box sx={{ width, ...sx.container, display: "flex", flexDirection: "column" }}>
            <List sx={{ display: "flex", flexDirection: "column", gap: "4px", flexGrow: 1 }}>
                {activeItems.map((item) => (
                    <SidebarItemRenderer
                        key={item.label}
                        item={item}
                        sx={sx}
                        navigate={navigate}
                        onItemClick={onItemClick}
                        depth={1}
                    />
                ))}
            </List>

            <Box
                sx={{
                    //marginTop: "auto",
                    paddingTop: theme.spacing(2),
                    paddingBottom: theme.spacing(2),
                    borderTop: `1px solid ${theme.palette.grey[800]}`,
                    display: "flex",
                    flexDirection: "column",
                    gap: theme.spacing(1),
                }}
            >
                <CacheIndicator />
                <StatusIndicator label="Kimmich" state={connectionStatus} />
                <StatusIndicator label="Immich" state="offline" />
            </Box>
        </Box>
    );
}


