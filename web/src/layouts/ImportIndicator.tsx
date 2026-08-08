import { useState } from "react";
import { Box, Typography, useTheme, keyframes } from "@mui/material";
import SyncIcon from "../components/icons/SyncIcon";
import CancelIcon from "@mui/icons-material/Clear";
import { useSseSelector } from "../sse/useSseSelector";
import { cancelImportForAll } from "../api/importer";
// Tilpass importen av useSseSelector til din prosjektstruktur:
// import { useSseSelector } from "../hooks/useSseSelector"; 

const spin = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
`;

export function ImportIndicator() {
    const theme = useTheme();
    const [cancelActive, setCancelActive] = useState(false);

    // Hent import-enhetene fra SSE-storen
    const importDevices = useSseSelector(state => state.importDevices) || {};

    // Sjekk om det er noen enheter som IKKE er ferdige (f.eks. Started eller Importing)
    const activeImports = Object.values(importDevices).filter(
        device => device.state !== "Completed"
    );

    const isImporting = activeImports.length > 0;

    // 1. Returner null umiddelbart hvis ingenting importeres
    if (!isImporting) {
        return null;
    }

    const handleCancelAll = () => {
        cancelImportForAll()
        // TODO: Kall API-et ditt for å avbryte alle (eller lag en løkke over activeImports.map(d => cancel(d.deviceId)))
        console.log("Canceling imports for:", activeImports.map(d => d.deviceId));
    };

    const handleClick = () => {
        if (!cancelActive) {
            // Første trykk: Vis cancel-knappen
            setCancelActive(true);
        } else {
            // Andre trykk: Utfør avbrudd
            handleCancelAll();
            setCancelActive(false);
        }
    };

    return (
        <Box
            onClick={handleClick}
            onMouseLeave={() => setCancelActive(false)}
            sx={{
                background: theme.palette.background.paper,
                p: 1,
                borderRadius: 10,
                display: "flex",
                flexDirection: "row",
                alignItems: "center",
                justifyContent: "space-between",
                minWidth: "150px",
                mr: 2,
                cursor: "pointer",
            }}
        >
            <Box sx={{ display: "flex", pl: 1, pr: 0.5 }}>
                <Typography>Importing</Typography>
            </Box>

            <Box
                sx={{
                    display: "flex",
                    position: "relative",
                    borderRadius: "50%",
                    bgcolor: cancelActive ? theme.palette.error.main : theme.palette.primary.main,
                    p: 0.6,
                    ml: 1,
                    alignItems: "center",
                    justifyContent: "center",
                    transition: "background-color 0.2s ease",
                    "&:hover": {
                        bgcolor: theme.palette.error.main,
                        "& .sync-icon": { opacity: 0 },
                        "& .cancel-icon": { opacity: 1 }
                    },
                    ...(cancelActive && {
                        "& .sync-icon": { opacity: 0 },
                        "& .cancel-icon": { opacity: 1 }
                    })
                }}
            >
                <SyncIcon className="sync-icon" sx={{
                    animation: `${spin} 1.5s linear infinite`,
                    color: theme.palette.primary.contrastText,
                    transition: "opacity 0.2s ease",
                    opacity: cancelActive ? 0 : 1,
                }} />

                <CancelIcon className="cancel-icon" sx={{
                    position: "absolute",
                    opacity: cancelActive ? 1 : 0,
                    color: theme.palette.error.contrastText,
                    transition: "opacity 0.2s ease",
                }} />
            </Box>
        </Box>
    );
}