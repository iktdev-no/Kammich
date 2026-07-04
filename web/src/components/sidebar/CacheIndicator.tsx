import { Box, Typography, LinearProgress, useTheme } from "@mui/material";
import { formatBytes } from "../../utils/format";

interface CacheIndicatorProps {
  used: number;   // bytes
  total: number;  // bytes
}

export function CacheIndicator({ used, total }: CacheIndicatorProps) {
  const theme = useTheme();

  const percent = total > 0 ? (used / total) * 100 : 0;

  return (
    <Box sx={{ 
      display: "flex", 
      flexDirection: "column", 
      gap: 1,
      padding: 2,
      mr: 2,
      ml: 2,
      borderRadius: 2,
      backgroundColor: theme.palette.background.paper,
      textAlign: "left"
    }}>
      <Typography
        variant="body2"
        sx={{ color: theme.palette.text.primary, fontWeight: 600 }}
      >
        Cache space
      </Typography>

      <Typography
        variant="body2"
        sx={{ color: theme.palette.grey[500] }}
      >
        {formatBytes(used)} of {formatBytes(total)} used
      </Typography>

      <LinearProgress
        variant="determinate"
        value={percent}
        sx={{
          height: 8,
          borderRadius: 4,
          backgroundColor: theme.palette.grey[800],
          "& .MuiLinearProgress-bar": {
            backgroundColor: theme.palette.primary.main,
          },
        }}
      />
    </Box>
  );
}
