import CableIcon from "@mui/icons-material/Cable";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelIcon from "@mui/icons-material/Cancel";
import { Box } from "@mui/material";
import HelpIcon from '@mui/icons-material/Help';

interface CableStatusIconProps {
    connected?: boolean | null;
}

export default function CableStatusIcon({
    connected,
}: CableStatusIconProps) {
    const StatusIcon =
        connected === true
            ? CheckCircleIcon
            : connected === false
                ? CancelIcon
                : HelpIcon;

    const statusColor =
        connected === true
            ? "success.main"
            : connected === false
                ? "error.main"
                : "text.secondary";

    return (
        <Box
            sx={{
                position: "relative",
                display: "inline-flex",
            }}
        >
            <CableIcon />

            <Box
                sx={{
                    position: "absolute",
                    right: -4,
                    bottom: -4,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    borderRadius: "50%",
                    backgroundColor: "background.paper",
                }}
            >
                <StatusIcon
                    sx={{
                        fontSize: 16,
                        color: statusColor,
                    }}
                />
            </Box>
        </Box>
    );
}