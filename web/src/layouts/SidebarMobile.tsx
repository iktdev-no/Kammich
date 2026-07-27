import {
  Drawer,
  useTheme,
} from "@mui/material";
import { OverlayScrollbarsComponent } from "overlayscrollbars-react";
import "overlayscrollbars/styles/overlayscrollbars.css";
import SidebarMenu from "../components/sidebar/SidebarMenu";

export interface SidebarMobileProps {
  visible: boolean;
  onClose: () => void;
}

export default function SidebarMobile({ visible, onClose }: SidebarMobileProps) {
  const theme = useTheme();
  const headerHeight = theme.layout.headerMobile;

  return (
    <Drawer
      open={visible}
      onClose={onClose}
      anchor="left"
      variant="persistent"
      keepMounted
      hideBackdrop={true}
      slotProps={{
        paper: {
          sx: {
            position: "absolute",
            top: `${headerHeight}px`,
            height: `calc(100vh - ${headerHeight}px)`,
            width: 251,
            backgroundColor: theme.palette.background.default,
            borderRight: "1px solid #333",
            overflow: "hidden", // Låser standard scroll og overlater rullingen til OverlayScrollbars
          },
        },
      }}
    >
      <OverlayScrollbarsComponent
        options={{
          scrollbars: {
            theme: "os-theme-light", // Juster til "os-theme-dark" om du foretrekker det basert på fargetema
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
        <SidebarMenu width={250} />
      </OverlayScrollbarsComponent>
    </Drawer>
  );
}