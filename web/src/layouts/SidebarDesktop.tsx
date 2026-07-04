import { Box, useTheme } from "@mui/material";
import SidebarMenu from "../menu/SidebarMenu";


export default function SidebarDesktop() {
    const theme = useTheme();
  const headerHeight = theme.layout.headerDesktop;

  return (
    <Box
      sx={{
        position: "fixed",
        top: `${headerHeight}px`,
        left: 0,
        width: 270,
        height: `calc(100vh - ${headerHeight}px)`,
      }}
    >
      <SidebarMenu width={270} />
    </Box>
  );
}