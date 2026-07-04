import { Box, useTheme } from "@mui/material";

interface StatusProps {
  label: string | undefined;
  state: "online" | "offline" | "connecting";
}

export function StatusIndicator({ label, state }: StatusProps) {
  const theme = useTheme();

  const colors = {
    online: theme.palette.success.main,
    offline: theme.palette.error.main,
    connecting: theme.palette.warning.main,
  };

  const formattedState = state.charAt(0).toUpperCase() + state.slice(1);
  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 1,
        padding: "6px 20px",
        borderRadius: 12,
        backgroundColor: colors[state] + "22", // toned pill
        color: colors[state],
        fontSize: 13,
        fontWeight: 600,
        margin: "0px 10px"
      }}
    >
      <Box
        sx={{
          width: 8,
          height: 8,
          borderRadius: "50%",
          backgroundColor: colors[state],
        }}
      />
      {label} {formattedState}
    </Box>
  );
}
