import React from "react";
import { Box, useTheme } from "@mui/material";

export default function MadeInNorwayBadge() {
    const theme = useTheme();

    return (
        <Box
            component="footer"
            sx={{
                mt: "auto",
                pt: 4,
                pb: 2,
                borderTop: `1px solid ${theme.palette.divider}`,
                display: "flex",
                justifyContent: "center",
                alignItems: "center"
            }}
        >
            {/* Minimalistisk SVG i stil med den offisielle Made in Norway-profilen */}
            <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 130 45"
                height="30"
                style={{ display: "block" }}
            >
                {/* Hvit bakgrunn begrenset nøyaktig til SVG-ens viewBox */}
                <rect width="130" height="45" fill="#ffffff" rx="3" />

                {/* Venstre kolonne: rød boks + rød sirkel */}
                <rect x="6" y="7" width="14" height="14" fill="#ba0c2f" />
                <circle cx="13" cy="31" r="7" fill="#ba0c2f" />

                {/* Hvite skillelinjer / kors-geometri */}
                <rect x="21" y="7" width="2" height="31" fill="#ffffff" />
                <rect x="6" y="22" width="47" height="2" fill="#ffffff" />

                {/* Midtre stripe (blå) */}
                <rect x="24" y="7" width="4" height="31" fill="#00205b" />

                {/* Høyre rektangel (stort rødt) */}
                <rect x="31" y="7" width="22" height="31" fill="#ba0c2f" />

                {/* Typografi: "MADE IN" */}
                <text
                    x="64"
                    y="19"
                    fontFamily="Inter, Roboto, sans-serif"
                    fontWeight="800"
                    fontSize="11"
                    fill="#00205b"
                    letterSpacing="1.5"
                >
                    MADE IN
                </text>

                {/* Typografi: "NORWAY" */}
                <text
                    x="64"
                    y="33"
                    fontFamily="Inter, Roboto, sans-serif"
                    fontWeight="800"
                    fontSize="11"
                    fill="#00205b"
                    letterSpacing="1.5"
                >
                    NORWAY
                </text>
            </svg>
        </Box>
    );
}