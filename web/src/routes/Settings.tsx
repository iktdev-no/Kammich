import React, { useState } from "react";
import { Box, List, ListItemButton, ListItemText, Typography, useTheme } from "@mui/material";
import WifiIcon from "@mui/icons-material/Wifi";
import SettingsIcon from "@mui/icons-material/Settings";
import { useSseSelector } from "../sse/useSseSelector";
import WifiSettings from "./settings/WifiSettings";

type SettingsTab = "wifi" | "system";

export default function Settings() {
    const theme = useTheme();
    const [activeTab, setActiveTab] = useState<SettingsTab>("wifi");
    
    // Beholder ping-meldingen din i bunnen av sub-menyen så vi ser at SSE lever
    const lastPing = useSseSelector(state => state.lastPing);

    return (
        <Box 
            sx={{ 
                display: "flex", 
                width: "100%", 
                height: "100%", 
                backgroundColor: theme.palette.background.default 
            }}
        >
            {/* Intern sub-meny for innstillinger (Immich-stil) */}
            <Box
                sx={{
                    width: "240px",
                    borderRight: `1px solid ${theme.palette.grey[800] || "#1f1f1f"}`,
                    display: "flex",
                    flexDirection: "column",
                    padding: theme.spacing(2),
                    gap: theme.spacing(2),
                    flexShrink: 0
                }}
            >
                <Typography 
                    variant="subtitle2" 
                    sx={{ 
                        px: 1, 
                        fontWeight: 700, 
                        color: theme.palette.text.secondary,
                        textTransform: "uppercase",
                        letterSpacing: "0.5px"
                    }}
                >
                    Innstillinger
                </Typography>

                <List sx={{ display: "flex", flexDirection: "column", gap: "4px", flexGrow: 1 }}>
                    <ListItemButton
                        selected={activeTab === "wifi"}
                        onClick={() => setActiveTab("wifi")}
                        sx={{
                            borderRadius: "4px",
                            color: activeTab === "wifi" ? theme.palette.text.primary : theme.palette.text.secondary,
                            "&.Mui-selected": {
                                backgroundColor: theme.palette.grey[900],
                                color: theme.palette.text.primary,
                                "&:hover": { backgroundColor: theme.palette.grey[800] }
                            }
                        }}
                    >
                        <WifiIcon sx={{ mr: 2, fontSize: "20px" }} />
                        <ListItemText 
                            primary="Wi-Fi" 
slotProps={{
        primary: {
            style: { 
                fontWeight: activeTab === "wifi" ? 600 : 400, 
                fontSize: "14px" 
            }
        }
    }}
                        />
                    </ListItemButton>

                    <ListItemButton
                        selected={activeTab === "system"}
                        onClick={() => setActiveTab("system")}
                        sx={{
                            borderRadius: "4px",
                            color: activeTab === "system" ? theme.palette.text.primary : theme.palette.text.secondary,
                            "&.Mui-selected": {
                                backgroundColor: theme.palette.grey[900],
                                color: theme.palette.text.primary,
                                "&:hover": { backgroundColor: theme.palette.grey[800] }
                            }
                        }}
                    >
                        <SettingsIcon sx={{ mr: 2, fontSize: "20px" }} />
                        <ListItemText 
                            primary="System" 
slotProps={{
        primary: {
            style: { 
                fontWeight: activeTab === "wifi" ? 600 : 400, 
                fontSize: "14px" 
            }
        }
    }}
                        />
                    </ListItemButton>
                </List>

                {/* Status/Ping i bunnen av menyen */}
                <Box 
                    sx={{ 
                        pt: 2, 
                        borderTop: `1px solid ${theme.palette.grey[900]}`,
                        px: 1
                    }}
                >
                    <Typography variant="caption" sx={{ color: theme.palette.text.disabled, display: "block" }}>
                        SSE Tilkobling:
                    </Typography>
                    <Typography variant="caption" sx={{ color: theme.palette.text.secondary, fontFamily: "monospace" }}>
                        {lastPing || "Venter på ping..."}
                    </Typography>
                </Box>
            </Box>

            {/* Hovedinnhold for det aktive panelet */}
            <Box 
                sx={{ 
                    flexGrow: 1, 
                    overflowY: "auto",
                    paddingLeft: theme.spacing(2) 
                }}
            >
                {activeTab === "wifi" && <WifiSettings />}
                {activeTab === "system" && (
                    <Box sx={{ padding: theme.spacing(4) }}>
                        <Typography variant="h5" sx={{ fontWeight: 600 }}>Systeminnstillinger</Typography>
                        <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 1 }}>
                            Her kan vi legge til nodestyring, omstart av tjenester eller lagringskonfigurasjon senere.
                        </Typography>
                    </Box>
                )}
            </Box>
        </Box>
    );
}