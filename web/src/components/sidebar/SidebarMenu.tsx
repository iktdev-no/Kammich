import { Box, List, useTheme } from "@mui/material";
import { useNavigate } from "react-router-dom";
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
import StorageIcon from "@mui/icons-material/Storage";
import type { SidebarItem } from "./SidebarItemTypes";
import { useSseSelector } from "../../sse/useSseSelector";
import { useEffect, useMemo } from "react";

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

    useEffect(() => {
        console.log(connectionStatus)
    }, [connectionStatus])

    // Bruk useMemo slik at menyen oppdateres kun når 'devices' endres
    const sidebarItems: SidebarItem[] = useMemo(() => [
        {
            label: "Photo",
            icon: PhotoLibraryOutlinedIcon,
            to: "/",
            children: devices.map(d => ({
                label: d.model ?? d.name,
                icon: (d.type !== "BLOCK") ? CameraAltOutlinedIcon : PermMediaOutlinedIcon,
                to: `/camera/${d.id}`,
            })),
        },
        { label: "Upload", icon: CloudUploadOutlinedIcon, to: "/upload" },
        { label: "Cache", icon: Inventory2OutlinedIcon },
        { label: "Settings", icon: SettingsIcon, to: "/settings" },
        {
            label: "Refresh",
            icon: StorageIcon,
            action: () => console.log("Refreshing storage…")
        },
    ], [devices]); // <-- Dependency: Re-kalkulerer kun når devices endres

    return (
        <Box sx={{ width, ...sx.container, display: "flex", flexDirection: "column" }}>
            <List sx={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                {sidebarItems.map((item) => (
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
                    marginTop: "auto",
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


