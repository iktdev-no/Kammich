import {
  Drawer,
  useTheme,
} from "@mui/material";
import SidebarMenu from "../components/sidebar/SidebarMenu";

export interface SidebarMobileProps {
  visible: boolean;
  onClose: () => void;
}

export default function SidebarMobile({ visible, onClose }: SidebarMobileProps) {
  const theme = useTheme();
  const headerHeight = theme.layout.headerMobile


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
            top: `${headerHeight}px`,                    // FIX: under header
            height: `calc(100vh - ${headerHeight}px)`,
            width: 240,
            backgroundColor: "#111",
            borderRight: "1px solid #333",
          },
        },
      }}
    >
      <SidebarMenu width={240} />
    </Drawer>
  );
}
