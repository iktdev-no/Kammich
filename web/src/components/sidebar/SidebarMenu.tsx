import { Box, List, useTheme } from "@mui/material";
import { useLocation, useNavigate } from "react-router-dom";
import { sidebarStyles } from "../../theme/sidebarTheme";
import { CacheIndicator } from "./CacheIndicator";
import { StatusIndicator } from "./StatusIndicator";
import { SidebarItemRenderer } from "./SidebarItemRendrer";
import CloudUploadOutlinedIcon from "@mui/icons-material/CloudUploadOutlined";
import SettingsIcon from "@mui/icons-material/Settings";
import WifiIcon from '@mui/icons-material/Wifi';
import WifiTetheringIcon from '@mui/icons-material/WifiTethering';
import PersonIcon from '@mui/icons-material/Person';
import PeopleIcon from '@mui/icons-material/People';
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera';
import PhotoLibraryIcon from '@mui/icons-material/PhotoLibrary';
import PhotoAlbumIcon from '@mui/icons-material/PhotoAlbum';
import SdStorageOutlinedIcon from '@mui/icons-material/SdStorageOutlined';
import CableIcon from '@mui/icons-material/Cable';
import type { SidebarItem } from "./SidebarItemTypes";
import { useSseSelector } from "../../sse/useSseSelector";
import { useEffect, useMemo, useState } from "react";
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import MemoryIcon from '@mui/icons-material/Memory';
import PublicIcon from '@mui/icons-material/Public';
import ImportIcon from '@mui/icons-material/SystemUpdateAlt';
import { getPhotoDevices } from "../../api/requests/photo";
import type { ImmichUserAccesses, PhotoDevice } from "../../types/types";
import ImmichIcon from "../icons/ImmichIcon";
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import SupervisedUserCircleIcon from '@mui/icons-material/SupervisedUserCircle';

export interface SidebarMenuProps {
    width: number;
    onItemClick?: () => void;
}

export default function SidebarMenu({ width, onItemClick }: SidebarMenuProps) {
    const theme = useTheme();
    const sx = sidebarStyles(theme);
    const navigate = useNavigate();
    const connectionStatus = useSseSelector(state => state.connectionStatus);
    const immichAvailability = useSseSelector(state => state.immichAvailability);


    const devices = useSseSelector(state => state.devices);
    const location = useLocation();
    const isSettings = location.pathname.startsWith("/settings");


    // State for enheter med lagrede bilder
    const [photoDevices, setPhotoDevices] = useState<PhotoDevice[]>([]);
    const immichAccesses = useState<ImmichUserAccesses[]>([]);

    const fetchPhotoDevices = () => {
        console.info("Henter photo enheter");
        getPhotoDevices()
            .then(data => setPhotoDevices(data))
            .catch(err => console.error("Klarte ikke å hente foto-enheter:", err));
    };

    // Hent enheter én gang ved oppstart
    useEffect(() => {
        fetchPhotoDevices();
    }, []);


    // Bruk useMemo slik at menyen oppdateres kun når 'devices' endres
    const mainMenuItems: SidebarItem[] = useMemo(() => [
        {
            label: "Photos",
            icon: PhotoLibraryIcon,
            to: "/",
            action: fetchPhotoDevices,
            children: photoDevices.map(d => ({
                label: d.model ?? d.name,
                icon: PhotoLibraryIcon,
                to: `/photo/${d.serialNumber}`,
            })),
        },
        {
            label: "Album",
            icon: PhotoAlbumIcon,
            to: "/album"
        },
        {
            label: "Devices",
            icon: CableIcon,
            to: "/devices",
            children: devices.map(d => ({
                label: d.model ?? d.name,
                icon: (d.interfaceType !== "BLOCK") ? PhotoCameraIcon : SdStorageOutlinedIcon,
                to: `/devices/${d.id}`,
            })),
        },
        {
            label: "Import",
            icon: ImportIcon,
            to: "/import",
            children: [
                {
                    label: "Ownership",
                    icon: SupervisedUserCircleIcon,
                    to: "/ownership"
                }
            ]
        },
        { label: "Upload", icon: CloudUploadOutlinedIcon, to: "/upload" },

        {
            label: "Settings",
            icon: SettingsIcon,
            to: "/settings",
            sx: { marginTop: "auto" }
        },

    ], [devices, photoDevices]);


    const settingsMenuItems: SidebarItem[] = useMemo(() => [
        {
            label: "Back",
            icon: ArrowBackRoundedIcon,
            to: "/",
        },
        {
            label: "Immich",
            icon: ImmichIcon,
            to: "/settings/immich",
            // Bruk en ternary eller short-circuit for å slå av/på barna
            children: immichAccesses && immichAccesses.length > 0 ? [
                {
                    label: "Me",
                    icon: PersonIcon,
                    to: "/settings/immich/me",
                    activeColor: "secondary"
                },
                {
                    label: "Users",
                    icon: PeopleIcon,
                    to: "/settings/immich/users",
                    activeColor: "secondary"
                },
                {
                    label: "Access",
                    icon: VpnKeyIcon,
                    to: `/settings/immich/access`,
                    activeColor: "secondary"
                },

            ] : undefined // eller [] avhengig av hva Sidebar-komponenten din liker best
        },
        {
            label: "Network",
            icon: PublicIcon,
            to: "/settings/networking",
            children: [
                {
                    label: "Wifi",
                    icon: WifiIcon,
                    to: "/settings/wifi"
                },
                {
                    label: "Tether",
                    icon: WifiTetheringIcon,
                    to: "/settings/ap"
                }
            ]
        },
        {
            label: "System",
            icon: MemoryIcon,
            to: "/settings/system",
            sx: { marginTop: "auto" }
        },
    ], [immichAccesses]);


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
                <StatusIndicator label="Kammich" state={connectionStatus} />
                <StatusIndicator
                    label="Immich"
                    state={(immichAvailability?.isAvailable && connectionStatus == "online") ? "online" : "offline"}
                />
            </Box>
        </Box>
    );
}


