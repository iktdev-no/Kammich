import { useTheme } from "@mui/material";
import type { UserAvatarColor } from "../types/types";

export function getAvatarColor(color: UserAvatarColor | undefined): string {
    const theme = useTheme();
    switch (color) {
        case "blue":
            return theme.palette.info.main;
        case "green":
            return theme.palette.success.main;
        case "pink":
            return "#E91E63"; // Egendefinert pink
        case "red":
            return theme.palette.error.main;
        case "yellow":
            return "#FFC107"; // Egendefinert yellow
        case "purple":
            return theme.palette.secondary.main;
        case "orange":
            return "#FF9800"; // Egendefinert orange
        case "gray":
            return theme.palette.grey[500];
        case "amber":
            return "#FFC107";
        case "primary":
        default:
            return theme.palette.primary.main;
    }
}