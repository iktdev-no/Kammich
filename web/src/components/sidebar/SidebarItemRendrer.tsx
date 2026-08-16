import {
    Box,
    Collapse,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    IconButton,
    useTheme,
    alpha,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import { NavLink } from "react-router-dom";
import { useState } from "react";
import type { SidebarItem } from "./SidebarItemTypes";

interface SidebarItemRendererProps {
    item: SidebarItem;
    sx: any;
    navigate: (to: string) => void;
    depth: number;
    onItemClick?: () => void;
}

export function SidebarItemRenderer({
    item,
    sx,
    navigate,
    onItemClick,
    depth,
}: SidebarItemRendererProps) {
    const theme = useTheme();
    const hasChildren = item.children && item.children.length > 0;
    const [open, setOpen] = useState(true);

    const paddingLeft = depth * 2;

    const handleClick = () => {
        if (item.action) item.action();
        if (item.to) navigate(item.to);
        if (onItemClick) onItemClick();
    };

    const toggle = (e: React.MouseEvent) => {
        e.stopPropagation();
        setOpen((prev) => !prev);
    };

    const ParentContent = ({ isActive }: { isActive: boolean }) => {
        // Sjekk om elementet skal ha secondary-farge ved aktiv tilstand
        const isSecondary = item.activeColor === "secondary";

        const customActiveBg = isSecondary
            ? alpha(theme.palette.secondary.main, 0.18)
            : sx.itemActive.backgroundColor;

        const customActiveColor = isSecondary
            ? theme.palette.secondary.main
            : sx.iconActive.color;

        return (
            <ListItemButton
                sx={{
                    ...sx.item,
                    pl: paddingLeft,
                    ...(isActive ? { ...sx.itemActive, backgroundColor: customActiveBg } : {}),
                    ...item.sx,
                }}
                onClick={handleClick}
            >
                <ListItemIcon sx={{ minWidth: 32 }}>
                    <item.icon
                        sx={isActive ? { color: customActiveColor } : sx.icon}
                    />
                </ListItemIcon>

                <ListItemText
                    primary={item.label}
                    sx={isActive ? { ...sx.textActive, color: customActiveColor } : sx.text}
                />

                {hasChildren && (
                    <IconButton
                        size="small"
                        onClick={(e) => {
                            e.stopPropagation();
                            e.preventDefault();
                            toggle(e);
                        }}
                        sx={{ ml: "auto", color: sx.icon.color }}
                    >
                        {open ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                    </IconButton>
                )}
            </ListItemButton>
        );
    };

    const children = hasChildren && (
        <Collapse in={open} unmountOnExit>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
                {item.children!.map((child) => (
                    <SidebarItemRenderer
                        key={child.label}
                        item={child}
                        sx={sx}
                        navigate={navigate}
                        onItemClick={onItemClick}
                        depth={depth + 1}
                    />
                ))}
            </Box>
        </Collapse>
    );

    // Fjernet <Box sx={item.sx}> rundt her, siden vi sprøyer inn item.sx direkte på ListItemButton i stedet
    if (item.to) {
        return (
            <Box>
                <NavLink to={item.to} style={{ textDecoration: "none" }}>
                    {({ isActive }) => <ParentContent isActive={isActive} />}
                </NavLink>
                {children}
            </Box>
        );
    }

    return (
        <Box>
            <ParentContent isActive={false} />
            {children}
        </Box>
    );
}