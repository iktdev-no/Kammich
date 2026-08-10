import React, { useState } from "react";
import { Box, List, ListItemButton, ListItemText, Typography, useTheme } from "@mui/material";
import WifiIcon from "@mui/icons-material/Wifi";
import SettingsIcon from "@mui/icons-material/Settings";
import { useSseSelector } from "../sse/useSseSelector";

type SettingsTab = "wifi" | "system";

export default function Settings() {
    const theme = useTheme();

    // Beholder ping-meldingen din i bunnen av sub-menyen så vi ser at SSE lever

    return (<></>
    );
}