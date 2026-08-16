import type { SxProps, Theme } from "@mui/material";

export interface SidebarItem {
  label: string;
  icon: React.ElementType;
  to?: string;
  action?: () => void;
  children?: SidebarItem[];
  sx?: SxProps<Theme>;
  color?: string;
  activeColor?: "primary" | "secondary";

}
