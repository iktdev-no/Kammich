import {
    Box,
    Card,
    CardContent,
    Chip,
    Divider,
    Stack,
    Typography,
} from "@mui/material";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutlineOutlined";
import ThermostatIcon from "@mui/icons-material/Thermostat";

import NvmeIcon from "../../components/icons/NvmeIcon";
import SkullIcon from "../../components/icons/SkullIcon";
import SsdIcon from "../../components/icons/SsdIcon";
import HddIcon from "../../components/icons/HddIcon";

import type { DiskHealth } from "../../types/types";

interface DiskHealthCardProps {
    health: DiskHealth;
}

const diskIcon = (disk: DiskHealth) => {
    if (disk.protocol.toLocaleLowerCase() === "nvme") {
        return NvmeIcon;
    }
    if (disk.diskVariant === "SSD") {
        return SsdIcon;
    } else return HddIcon;
}

export default function DiskHealthCard({
    health,
}: DiskHealthCardProps) {
    const DiskIcon = diskIcon(health);

    return (
        <Card
            sx={{
                height: "100%",
                borderRadius: 3,
            }}
        >
            <CardContent
                sx={{
                    p: 2.5,
                    "&:last-child": {
                        pb: 2.5,
                    },
                }}
            >
                <Stack spacing={2}>
                    <Stack
                        direction="row"
                        spacing={1.5}
                        sx={{
                            alignItems: "center",
                        }}
                    >
                        <DiskIcon
                            sx={{
                                fontSize: 32,
                                flexShrink: 0,
                            }}
                        />

                        <Box
                            sx={{
                                minWidth: 0,
                                flex: 1,
                            }}
                        >
                            <Typography
                                noWrap
                                sx={{
                                    fontWeight: 600,
                                }}
                            >
                                {health.modelName}
                            </Typography>

                            <Typography
                                variant="body2"
                                color="text.secondary"
                                noWrap
                            >
                                {health.protocol} ·{" "}
                                {health.deviceName}
                            </Typography>
                        </Box>
                    </Stack>

                    <Divider />

                    <Stack
                        direction="row"
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
                                spacing={0.75}
                                sx={{
                                    alignItems: "center",
                                }}
                            >
                                <ThermostatIcon
                                    fontSize="small"
                                />

                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                >
                                    Temperature
                                </Typography>
                            </Stack>

                            <Typography
                                variant="h6"
                                sx={{
                                    fontWeight: 600,
                                }}
                            >
                                {health.temperatureCelsius} °C
                            </Typography>
                        </Box>

                        <Box
                            sx={{
                                flex: 1,
                                minWidth: 0,
                            }}
                        >
                            <Stack
                                direction="row"
                                spacing={0.75}
                                sx={{
                                    alignItems: "center",
                                }}
                            >
                                <SkullIcon
                                    sx={{
                                        fontSize: 20,
                                    }}
                                />

                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                >
                                    Endurance used
                                </Typography>
                            </Stack>

                            <Typography
                                variant="h6"
                                sx={{
                                    fontWeight: 600,
                                }}
                            >
                                {health.percentageUsed}%
                            </Typography>
                        </Box>
                    </Stack>

                    <Chip
                        icon={
                            health.isHealthy
                                ? <CheckCircleIcon />
                                : <ErrorOutlineIcon />
                        }
                        label={
                            health.isHealthy
                                ? "Healthy"
                                : "Critical"
                        }
                        color={
                            health.isHealthy
                                ? "success"
                                : "error"
                        }
                        variant="outlined"
                    />
                </Stack>
            </CardContent>
        </Card>
    );
}