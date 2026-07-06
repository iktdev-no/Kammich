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
// theme.ts

export const getTheme = (mode: 'light' | 'dark') => createTheme({
  palette: {
    mode,
    primary: { main: "#4f8cff" },
    secondary: { main: "#ff4dca" },
    background: {
      default: mode === 'light' ? "#ffffff" : "#000000",
      paper: mode === 'light' ? "#f5f5f5" : "#1e1e1e",
    },
  },
  shape: { borderRadius: 8 },
  layout: {
    headerMobile: 62,
    headerDesktop: 76,
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          transition: "background-color 0.35s ease, color 0.35s ease",
        },
        "*": {
          transition: "background-color 0.25s ease, color 0.25s ease, border-color 0.25s ease",
        },
      },
    },
  },
});
