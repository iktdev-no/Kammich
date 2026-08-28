import { Box } from "@mui/material";

export default function MadeInNorwayBadge() {

    return (
        <Box
            component="footer"
            sx={{
                mt: "auto",
                pt: 4,
                pb: 2,
                display: "flex",
                justifyContent: "center",
                alignItems: "center"
            }}
        >
            <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 130 45"
                height="30"
                style={{ display: "block" }}
            >
                {/* Hvit bakgrunn for hele badge-en */}
                <rect width="130" height="45" fill="#ffffff" rx="3" />

                {/* Innfelt norsk flagg på venstre side (skalert opp fra 22x16 til 47x35 for å passe formatet) */}
                <g transform="translate(6, 5)">
                    <rect width="47" height="35" fill="#ba0c2f" rx="1" />
                    <path d="M0,17.5h47M17.5,0v35" stroke="#fff" stroke-width="8.75" />
                    <path d="M0,17.5h47M17.5,0v35" stroke="#00205b" stroke-width="4.375" />
                </g>

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

    )
}