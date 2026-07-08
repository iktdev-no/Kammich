import { Box, Typography, LinearProgress, useTheme } from "@mui/material";
import { formatBytes } from "../../utils/format";
import { useSseSelector } from "../../sse/useSseSelector";
import type { MediaStats } from "../../types/types";

export function CacheIndicator() {
  const theme = useTheme();
  const mediaStat: MediaStats|undefined = useSseSelector(state => state.internalMediaStats);

  if (!mediaStat) {
    return null;
  }

  return (
    <Box sx={{ padding: 2, m:1.5, borderRadius: 2, backgroundColor: theme.palette.background.paper }}>
      <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>Cache space</Typography>
      <Box key={mediaStat.serial} sx={{ mb: 0}}>
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            {mediaStat.manufacturer}
          </Typography>
          
          {/* Aggregert linje for hele disken */}
          <StorageUsageBar 
            used={mediaStat.usedBytes} 
            total={mediaStat.totalBytes} 
            label="Total usage" 
          />
          <Typography variant="caption" sx={{ color: "text.secondary" }}>
            {mediaStat.model} 
          </Typography>
        </Box>
    </Box>
  );
}

function StorageUsageBar({ used, total, label }: { used: number, total: number, label: string }) {
  const theme = useTheme();
  const percent = total > 0 ? (used / total) * 100 : 0;
  
  const color = percent < 70 ? theme.palette.success.main : percent < 90 ? theme.palette.warning.main : theme.palette.error.main;

  return (
    <Box sx={{ mt: 0.5 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="caption">{label}</Typography>
        <Typography variant="caption">{formatBytes(used)} / {formatBytes(total)}</Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={percent}
        sx={{
          height: 6,
          borderRadius: 3,
          backgroundColor: theme.palette.grey[800],
          "& .MuiLinearProgress-bar": { backgroundColor: color }
        }}
      />
    </Box>
  );
}