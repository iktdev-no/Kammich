import { Card, CardContent, Stack, Typography, Box, Chip } from "@mui/material";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutlineOutlined";
import type { DiskHealth } from "../../types/types";

interface DiskHealthOverviewCardProps {
    disks: DiskHealth[]
}

export default function DiskHealthOverviewCard({ disks }: DiskHealthOverviewCardProps) {
    const healthyDisks = disks.filter(
        disk => disk.isHealthy
    ).length;

    const unhealthyDisks =
        disks.length - healthyDisks;

    return (
        <Card
            sx={{
                borderRadius: 3,
            }}
        >
            <CardContent
                sx={{
                    p: {
                        xs: 2.5,
                        sm: 3,
                    },
                    "&:last-child": {
                        pb: {
                            xs: 2.5,
                            sm: 3,
                        },
                    },
                }}
            >
                <Stack spacing={2}>
                    <Typography
                        variant="h6"
                        sx={{
                            fontWeight: 600,
                        }}
                    >
                        Disk health
                    </Typography>

                    <Stack
                        direction={{
                            xs: "column",
                            sm: "row",
                        }}
                        spacing={2}
                    >
                        <Box
                            sx={{
                                flex: 1,
                                minWidth: 0,
                            }}
                        >
                            <Stack
                                direction="row"
                                spacing={1.5}
                                sx={{
                                    alignItems:
                                        "center",
                                }}
                            >
                                {unhealthyDisks ===
                                    0 ? (
                                    <CheckCircleIcon
                                        color="success"
                                    />
                                ) : (
                                    <ErrorOutlineIcon
                                        color="warning"
                                    />
                                )}

                                <Box>
                                    <Typography
                                        sx={{
                                            fontWeight:
                                                600,
                                        }}
                                    >
                                        {unhealthyDisks ===
                                            0
                                            ? "All disks healthy"
                                            : "Disk health warning"}
                                    </Typography>

                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                    >
                                        {disks.length}{" "}
                                        {disks.length ===
                                            1
                                            ? "disk"
                                            : "disks"}{" "}
                                        detected
                                    </Typography>
                                </Box>
                            </Stack>
                        </Box>

                        <Box
                            sx={{
                                display: "flex",
                                alignItems:
                                    "center",
                                justifyContent: {
                                    xs: "flex-start",
                                    sm: "flex-end",
                                },
                            }}
                        >
                            <Chip
                                icon={
                                    unhealthyDisks ===
                                        0 ? (
                                        <CheckCircleIcon />
                                    ) : (
                                        <ErrorOutlineIcon />
                                    )
                                }
                                label={
                                    unhealthyDisks ===
                                        0
                                        ? `${healthyDisks} healthy`
                                        : `${unhealthyDisks} warning`
                                }
                                color={
                                    unhealthyDisks ===
                                        0
                                        ? "success"
                                        : "warning"
                                }
                                variant="outlined"
                            />
                        </Box>
                    </Stack>
                </Stack>
            </CardContent>
        </Card>
    )
}