import {
    Box,
    Card,
    CardContent,
    Collapse,
    IconButton,
    Stack,
    Typography,
} from "@mui/material";
import { useState, type ReactNode } from "react";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";

export type NetworkServiceStatus =
    | "Online"
    | "Degraded"
    | "Offline";

interface NetworkServiceCardProps {
    icon: ReactNode;
    title: string;
    hostname?: string;
    status: NetworkServiceStatus;
    children?: ReactNode;
}

export default function NetworkServiceCard({
    icon,
    title,
    hostname,
    status,
    children,
}: NetworkServiceCardProps) {
    const [isCollapsed, setIsCollapsed] = useState(false);

    const statusColor = {
        Online: "success.main",
        Degraded: "warning.main",
        Offline: "action.disabledBackground",
    }[status];

    const statusTextColor = {
        Online: "success.contrastText",
        Degraded: "warning.contrastText",
        Offline: "text.primary",
    }[status];

    return (
        <Card
            sx={{
                backgroundColor: "background.paper",
                borderRadius: 3,
                border: "1px solid rgba(255,255,255,0.06)",
                transition: "all 0.2s ease-in-out",
                "&:hover": {
                    borderColor: "rgba(255,255,255,0.15)",
                    boxShadow: "0 4px 20px rgba(0,0,0,0.2)",
                },
            }}
        >
            <CardContent>
                <Stack
                    sx={{
                        flexDirection: "row",
                        alignItems: "center",
                        gap: 2,
                        cursor: "pointer",
                    }}
                    onClick={() => setIsCollapsed(prev => !prev)}
                >
                    <Box
                        sx={{
                            p: 1.5,
                            borderRadius: 2,
                            bgcolor: statusColor,
                            color: statusTextColor,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                        }}
                    >
                        {icon}
                    </Box>

                    <Box sx={{ flexGrow: 1 }}>
                        <Typography
                            variant="h6"
                            sx={{ fontWeight: 600 }}
                        >
                            {title}
                        </Typography>

                        {hostname && (
                            <Typography
                                variant="caption"
                                color="text.secondary"
                                sx={{ fontFamily: "monospace" }}
                            >
                                {hostname}
                            </Typography>
                        )}
                    </Box>

                    <IconButton
                        onClick={event => {
                            event.stopPropagation();
                            setIsCollapsed(prev => !prev);
                        }}
                    >
                        <ExpandMoreIcon
                            sx={{
                                transform: isCollapsed
                                    ? "rotate(0deg)"
                                    : "rotate(180deg)",
                                transition: "transform 0.2s",
                            }}
                        />
                    </IconButton>
                </Stack>

                <Collapse
                    in={!isCollapsed}
                    unmountOnExit
                >
                    <Box
                        sx={{
                            mt: 2,
                            display: "flex",
                            flexDirection: "column",
                            gap: 1.5,
                        }}
                    >
                        {children}
                    </Box>
                </Collapse>
            </CardContent>
        </Card>
    );
}