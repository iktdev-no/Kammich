import { Box, useTheme } from "@mui/material";
import { OverlayScrollbarsComponent } from "overlayscrollbars-react";
import "overlayscrollbars/styles/overlayscrollbars.css";
import SidebarMenu from "../components/sidebar/SidebarMenu";

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
        overflow: "hidden",
      }}
    >
      <OverlayScrollbarsComponent
        options={{
          scrollbars: {
            theme: "os-theme-light",
            visibility: "auto",
            autoHide: "scroll",
            autoHideDelay: 1300,
          },
        }}
        style={{
          height: "100%",
          width: "100%",
        }}
        defer
      >
        <SidebarMenu width={270} />
      </OverlayScrollbarsComponent>
    </Box>
  );
}