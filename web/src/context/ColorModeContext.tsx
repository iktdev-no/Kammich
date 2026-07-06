import { createContext, useContext, useMemo, useState } from "react";
import { ThemeProvider, CssBaseline } from "@mui/material";
import { getTheme } from "../theme/theme"; // Importer funksjonen din

const ColorModeContext = createContext({ toggleColorMode: () => {}, mode: 'dark' });

// ColorModeContext.tsx

export const ColorModeProvider = ({ children }: { children: React.ReactNode }) => {
  const [mode, setMode] = useState<'light' | 'dark'>('dark');

  const toggleColorMode = () => setMode((prev) => (prev === 'light' ? 'dark' : 'light'));

  // Bruker funksjonen fra theme.ts her
  const theme = useMemo(() => getTheme(mode), [mode]);

  return (
    <ColorModeContext.Provider value={{ toggleColorMode, mode }}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
};

export const useColorMode = () => useContext(ColorModeContext);
