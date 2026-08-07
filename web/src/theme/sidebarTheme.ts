import type { Theme } from "@mui/material";
import { alpha } from "@mui/material/styles";

export const sidebarStyles = (theme: Theme) => ({
  container: {
    backgroundColor: theme.palette.background.default,
    paddingTop: theme.spacing(1),
    paddingBottom: theme.spacing(1),

    // Desktop default
    height: `calc(100vh - ${theme.layout.headerDesktop}px)`,

    // Mobile override
    [theme.breakpoints.down("sm")]: {
      height: `calc(100vh - ${theme.layout.headerMobile}px)`,
    },
  },

  item: {
    marginRight: theme.spacing(3),
    borderTopLeftRadius: 0,
    borderBottomLeftRadius: 0,
    borderTopRightRadius: 25,
    borderBottomRightRadius: 25,

    paddingLeft: theme.spacing(3),
    paddingRight: theme.spacing(2),

    "& .MuiListItemIcon-root": {
      minWidth: 0,
      marginRight: theme.spacing(1),
    },

    "&:hover": {
      backgroundColor: alpha(theme.palette.primary.main, 0.12),
    },
  },

  itemActive: {
    backgroundColor: alpha(theme.palette.primary.main, 0.18),
    borderTopLeftRadius: 0,
    borderBottomLeftRadius: 0,
    borderTopRightRadius: 25,
    borderBottomRightRadius: 25,
  },

  icon: {
    // Bruk text.secondary i stedet for fast grå, eller f.eks. grey[600] for mørkere variant
    color: theme.palette.text.secondary,
  },

  iconActive: {
    color: theme.palette.primary.main,
  },

  text: {
    // text.secondary sikrer god kontrast i både lyst og mørkt tema
    color: theme.palette.text.secondary,
    marginLeft: 3,
  },

  textActive: {
    color: theme.palette.primary.main,
    fontWeight: 600,
    marginLeft: 3,
  },
});