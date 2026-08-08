import {
    Typography, Box, keyframes,
    useTheme
} from "@mui/material";
import SyncIcon from "../components/icons/SyncIcon";
import CancelIcon from '@mui/icons-material/Clear';

// 1. Definer keyframes for rotasjon
const spin = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
`;

export default function ImportIndicator() {
    const theme = useTheme();

    return (
        <Box sx={{
            background: theme.palette.background.paper,
            p: 1,
            borderRadius: 10,
            display: "flex",
            flexDirection: "row",
            alignContent: "center",
            alignItems: "center",
            justifyContent: "flex-end",
            flexWrap: "nowrap",
            mr: 2,
            gap: 2
        }}>
            <Box sx={{ display: "flex", flexDirection: "row", flexWrap: "nowrap", alignContent: "center", alignItems: "center", justifyContent: "flex-start", ml: 1, gap: 1 }}>
                <Typography>Importing</Typography>

                <Typography>Device</Typography>
            </Box>


            <Box sx={{
                display: "flex",
                position: "relative", // Viktig for å posisjonere Cancel over Sync
                borderRadius: "50%",
                bgcolor: theme.palette.primary.main,
                p: 0.6,
                ml: 1,
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer", // Vis musepeker ved hover
                // Vis cancel-ikon og endre farge ved hover på denne boksen:
                "&:hover": {
                    bgcolor: theme.palette.error.main, // Bytter bakgrunn til rød ved hover
                    "& .sync-icon": {
                        animation: "none", // Stopper spinning ved hover (valgfritt)
                        opacity: 0,        // Skjuler sync-ikonet
                    },
                    "& .cancel-icon": {
                        opacity: 1,        // Viser cancel-ikonet
                    }
                }
            }}>
                {/* Sync-ikonet som snurrer */}
                <SyncIcon className="sync-icon" sx={{
                    animation: `${spin} 1.5s linear infinite`,
                    color: theme.palette.primary.contrastText,
                    transition: "opacity 0.2s ease",
                }} />

                {/* Cancel-ikonet som er skjult til man hovrer */}
                <CancelIcon className="cancel-icon" sx={{
                    position: "absolute",
                    opacity: 0, // Usynlig som standard
                    color: theme.palette.error.contrastText,
                    transition: "opacity 0.2s ease",
                }} />
            </Box>
        </Box>
    )
}