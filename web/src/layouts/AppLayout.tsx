import { Outlet } from "react-router-dom";
import { Box, useTheme } from "@mui/material";
import { useState } from "react";
import Header from "./Header";
import { useIsMobile } from "../hooks/useIsMobile";
import SidebarMobile from "./SidebarMobile";
import SidebarDesktop from "./SidebarDesktop";

export default function AppLayout() {
  const isMobile = useIsMobile();
  const theme = useTheme();
  const [visible, setVisible] = useState(false); // only used for mobile drawer

  const headerHeight = isMobile
  ? theme.layout.headerMobile
  : theme.layout.headerDesktop;



  return (
    <Box sx={{ display: "flex", height: "100vh", width: "100vw" }}>
      <Header onToggleSidebar={() => setVisible(!visible)} />

      {isMobile ? (
        <SidebarMobile visible={visible} onClose={() => setVisible(false)} />
      ) : (
        <SidebarDesktop />
      )}

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          overflowY: "auto",
          paddingTop: `${headerHeight}px`,
          marginLeft: isMobile ? 0 : "240px", // desktop sidebar always open
          transition: "margin-left 0.2s ease",
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}
