import StorageIcon from "@mui/icons-material/Storage";
import SettingsIcon from "@mui/icons-material/Settings";

import {
  Box,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  useTheme,
} from "@mui/material";
import { NavLink, useNavigate } from "react-router-dom";
import { sidebarStyles } from "../theme/sidebarTheme";
import { StatusIndicator } from "../components/sidebar/StatusIndicator";
import { CacheIndicator } from "../components/sidebar/CacheIndicator";

export interface SidebarMenuProps {
  width: number;
  onItemClick?: () => void;
}

export default function SidebarMenu({ width, onItemClick }: SidebarMenuProps) {
  const theme = useTheme();
  const sx = sidebarStyles(theme);
  const navigate = useNavigate();

  return (
    <Box sx={{ width, ...sx.container,
                display: "flex",
        flexDirection: "column",
        height: "100%",
     }}>
      <List sx={{ display: "flex", flexDirection: "column", gap: "4px" }}>
        {sidebarItems.map(({ label, to, icon: Icon, action }) => {
          const handleClick = () => {
            if (action) action();         // run callback
            if (to) navigate(to);         // navigate if route exists
            if (onItemClick) onItemClick(); // close mobile drawer
          };

          // If item has a route, use NavLink for active styling
          if (to) {
            return (
              <NavLink key={label} to={to} style={{ textDecoration: "none" }}>
                {({ isActive }) => (
                  <ListItemButton
                    onClick={handleClick}
                    sx={{
                      ...sx.item,
                      ...(isActive ? sx.itemActive : {}),
                    }}
                  >
                    <ListItemIcon>
                      <Icon sx={isActive ? sx.iconActive : sx.icon} />
                    </ListItemIcon>
                    <ListItemText
                      primary={label}
                      sx={isActive ? sx.textActive : sx.text}
                    />
                  </ListItemButton>
                )}
              </NavLink>
            );
          }

          // If item has no route, render a normal button
          return (
            <ListItemButton
              key={label}
              onClick={handleClick}
              sx={sx.item}
            >
              <ListItemIcon>
                <Icon sx={sx.icon} />
              </ListItemIcon>
              <ListItemText primary={label} sx={sx.text} />
            </ListItemButton>
          );
        })}
      </List>

      {/* Bottom items — always at bottom */}
        <Box
          sx={{
            marginTop: "auto", // ⭐ pushes this to the bottom
            paddingTop: theme.spacing(2),
            paddingBottom: theme.spacing(2),
            borderTop: `1px solid ${theme.palette.grey[800]}`,
            display: "flex",
            flexDirection: "column",
            gap: theme.spacing(1),
          }}
        >
            <CacheIndicator used={5} total={100} />
                    <StatusIndicator label="Kimmich Online" state="online" />
          <StatusIndicator label="Immich Online" state="online" />
        </Box>
    </Box>
  );
}



export interface SidebarItem {
  label: string;
  icon: React.ElementType;
  to?: string;               // optional navigation
  action?: () => void;       // optional callback
}

export const sidebarItems: SidebarItem[] = [
  {
    label: "Storage",
    icon: StorageIcon,
    to: "/",                  // navigates
  },
  {
    label: "Settings",
    icon: SettingsIcon,
    to: "/settings",          // navigates
  },
  {
    label: "Refresh",
    icon: StorageIcon,
    action: () => {
      console.log("Refreshing storage…");
    },                        // performs an action
  },
];