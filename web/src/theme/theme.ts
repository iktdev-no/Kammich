import { createTheme } from "@mui/material/styles";

// --- TypeScript utvidelse av Theme ---
declare module "@mui/material/styles" {
  interface Theme {
    layout: {
      headerMobile: number;
      headerDesktop: number;
    };
  }

  interface ThemeOptions {
    layout?: {
      headerMobile?: number;
      headerDesktop?: number;
    };
  }
}

// --- Selve theme-definisjonen ---
export const theme = createTheme({
  palette: {
    mode: "dark",
    primary: { main: "#4f8cff" },
    secondary: { main: "#ff4dca" },
    background: {
      default: "#000000",
      paper: "#1e1e1e",
    },
  },

  shape: { borderRadius: 8 },

  layout: {
    headerMobile: 62,
    headerDesktop: 76,
  },
});
