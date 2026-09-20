import { Box, Typography } from "@mui/material";

interface NetworkDetailProps {
    label: string;
    value: string;
    emphasis?: boolean;
    monospace?: boolean;
}

export default function NetworkDetail({
    label,
    value,
    emphasis = false,
    monospace = false,
}: NetworkDetailProps) {
    return (
        <Box
            sx={{
                flex: "1 1 130px",
                minWidth: 0,
                px: 1.25,
                py: 1,
                borderRadius: 2,
                backgroundColor: "rgba(255, 255, 255, 0.04)",
                border: "1px solid",
                borderColor: "rgba(255, 255, 255, 0.06)",
            }}
        >
            <Typography
                variant="caption"
                color="text.secondary"
                sx={{
                    display: "block",
                    lineHeight: 1,
                    mb: 0.5,
                }}
            >
                {label}
            </Typography>

            <Typography
                variant="body2"
                sx={{
                    fontWeight: emphasis ? 600 : 500,
                    fontFamily: monospace ? "monospace" : undefined,
                }}
            >
                {value}
            </Typography>
        </Box>
    );
}