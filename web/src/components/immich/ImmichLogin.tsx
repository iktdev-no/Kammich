import { useState, type FormEvent } from "react";
import { Box, Paper, TextField, Button, Typography, Alert } from "@mui/material";
import { ImmichBrandLogo } from "../../components/icons/ImmichBrand";
import { immichLoginNormalFLow } from "../../api/requests/immich";
import type { ImmichUserMe } from "../../types/types";

interface ImmichLoginProps {
    onLoginSuccess?: (login: ImmichUserMe) => void;
}

export default function ImmichLogin({ onLoginSuccess }: ImmichLoginProps) {
    const [serverUrl, setServerUrl] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const handleLogin = async (e: FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        const cleanServerUrl = serverUrl.trim().replace(/\/$/, "");

        try {
            // Bruker din nye felles-funksjon mot bakenden som rydder opp i CORS og genererer API-nøkkel
            const response = await immichLoginNormalFLow({
                address: cleanServerUrl,
                email,
                password,
            });

            // Avhengig av hva KammichLoginResponse returnerer (her forventer vi f.eks. at den inneholder apiKey eller lignende)
            if (onLoginSuccess && response) {
                onLoginSuccess(response);
            }
        } catch (err: any) {
            // Hent ut en pen feilmelding enten fra err.message, response data eller standardtekst
            let errorMessage = "Noe gikk galt under innlogging";

            if (typeof err === "string") {
                errorMessage = err;
            } else if (err?.message) {
                errorMessage = err.message;
            } else if (err?.error) {
                errorMessage = err.error; // Hvis backenden returnerte { error: "melding" }
            }

            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Paper
            elevation={0}
            component="form"
            onSubmit={handleLogin}
            sx={{
                p: { xs: 3, sm: 5 },
                width: "100%",
                maxWidth: 440,
                bgcolor: "#141414",
                color: "white",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 3,
                borderRadius: 4,
                border: "1px solid #262626",
                my: "auto",
            }}
        >
            {/* Logo */}
            <Box sx={{ mt: 1, display: "flex", justifyContent: "center" }}>
                <ImmichBrandLogo sx={{ width: "auto", height: 64 }} />
            </Box>

            {/* Overskrift */}
            <Typography
                variant="h4"
                sx={{
                    fontWeight: 600,
                    color: "#f0f0f0",
                    fontSize: "1.8rem",
                    letterSpacing: "-0.5px"
                }}
            >
                Logg inn
            </Typography>

            {error && (
                <Alert severity="error" sx={{ width: "100%", bgcolor: "#2c1515", color: "#ff8080" }}>
                    {error}
                </Alert>
            )}

            <Box sx={{ width: "100%", display: "flex", flexDirection: "column", gap: 2.5 }}>
                <TextField
                    label="Serveradresse (f.eks. http://192.168.1.50:2283)"
                    type="url"
                    variant="filled"
                    fullWidth
                    required
                    value={serverUrl}
                    onChange={(e) => setServerUrl(e.target.value)}
                    slotProps={{
                        input: {
                            disableUnderline: true,
                            sx: {
                                bgcolor: "#1e222b !important",
                                borderRadius: 2,
                                color: "white",
                                height: 56,
                            }
                        }
                    }}
                    sx={{
                        "& .MuiInputLabel-root": { color: "#8c94a0", fontWeight: 400, fontSize: "0.9rem" },
                        "& .MuiInputLabel-root.Mui-focused": { color: "#8ab4f8" },
                    }}
                />

                <TextField
                    label="E-postadresse"
                    type="email"
                    variant="filled"
                    fullWidth
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="email"
                    slotProps={{
                        input: {
                            disableUnderline: true,
                            sx: {
                                bgcolor: "#1e222b !important",
                                borderRadius: 2,
                                color: "white",
                                height: 56,
                            }
                        }
                    }}
                    sx={{
                        "& .MuiInputLabel-root": { color: "#8c94a0", fontWeight: 400 },
                        "& .MuiInputLabel-root.Mui-focused": { color: "#8ab4f8" },
                    }}
                />

                <TextField
                    label="Passord"
                    type="password"
                    variant="filled"
                    fullWidth
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="current-password"
                    slotProps={{
                        input: {
                            disableUnderline: true,
                            sx: {
                                bgcolor: "#1e222b !important",
                                borderRadius: 2,
                                color: "white",
                                height: 56,
                            }
                        }
                    }}
                    sx={{
                        "& .MuiInputLabel-root": { color: "#8c94a0", fontWeight: 400 },
                        "& .MuiInputLabel-root.Mui-focused": { color: "#8ab4f8" },
                    }}
                />
            </Box>

            <Button
                type="submit"
                variant="contained"
                fullWidth
                disabled={loading}
                sx={{
                    mt: 1,
                    py: 1.6,
                    bgcolor: "#a8c7fa",
                    color: "#001d35",
                    fontWeight: 600,
                    fontSize: "0.95rem",
                    borderRadius: "50px",
                    boxShadow: "none",
                    textTransform: "none",
                    "&:hover": {
                        bgcolor: "#9bbcf6",
                        boxShadow: "none",
                    },
                }}
            >
                {loading ? "Logger inn..." : "Logg inn"}
            </Button>
        </Paper>
    );
}