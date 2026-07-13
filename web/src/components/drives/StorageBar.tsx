import { Box, Typography, LinearProgress, useTheme } from "@mui/material";
import { formatBytes } from "../../utils/format";
import type { StorageInfo } from "../../types/types";

interface StorageBarProps {
  storage: StorageInfo;
}

export function StorageBar({ storage }: StorageBarProps) {
  const theme = useTheme();
  const { stats, health } = storage;
  const used = stats.totalBytes - stats.freeBytes;

  const getProgressColor = (p: number) => {
    if (p < 70) return theme.palette.success.main;
    if (p < 90) return theme.palette.warning.main;
    return theme.palette.error.main;
  };

  return (
    <Box sx={{ mb: 2 }}>
      <Typography variant="body2" sx={{ fontWeight: 500, color: "text.primary" }}>
        {health.deviceName} ({health.protocol})
      </Typography>
      <Typography variant="caption" sx={{ color: "text.secondary" }}>
        {formatBytes(used)} / {formatBytes(stats.totalBytes)} brukt
      </Typography>
      <LinearProgress
        variant="determinate"
        value={stats.percentUsed}
        sx={{
          height: 8,
          borderRadius: 4,
          mt: 0.5,
          backgroundColor: theme.palette.grey[800],
          "& .MuiLinearProgress-bar": {
            backgroundColor: getProgressColor(stats.percentUsed),
            transition: "background-color 0.3s ease"
          },
        }}
      />
    </Box>
  );
}