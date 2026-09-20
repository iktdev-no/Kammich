import { useState } from "react";
import {
    Box,
    Button,
    Divider,
    TextField,
} from "@mui/material";

import type { WifiNetwork } from "../../../types/types";
import { wifiApi } from "../../../api/requests/networking/wifi";

interface WifiNetworkActionsCardProps {
    isConnected: boolean;
    wifi: WifiNetwork;
    onConnect: () => void;
}

export default function WifiNetworkActionsCard({
    isConnected,
    wifi,
    onConnect,
}: WifiNetworkActionsCardProps) {
    const [password, setPassword] =
        useState<string | undefined>(undefined);

    const handleConnect = (): void => {
        void wifiApi.connect(
            wifi.interfaceName,
            wifi.bssid,
            password,
        );

        onConnect();
    };

    const handleDisconnect = (): void => {
        void wifiApi.disconnect(wifi.interfaceName);
    };

    return (
        <>
            <Divider />

            <Box
                sx={{
                    display: "flex",
                    gap: 1,
                    borderRadius: 1,
                    p: 2,
                }}
            >
                {isConnected ? (
                    <Button
                        variant="contained"
                        color="error"
                        size="small"
                        fullWidth
                        onClick={handleDisconnect}
                    >
                        Disconnect
                    </Button>
                ) : (
                    <>
                        {wifi.isSecure && (
                            <TextField
                                size="small"
                                label="Password"
                                type="password"
                                fullWidth
                                value={password}
                                onChange={event =>
                                    setPassword(event.target.value)
                                }
                            />
                        )}

                        <Button
                            variant="contained"
                            size="small"
                            onClick={handleConnect}
                            sx={{
                                textTransform: "none",
                                px: 3,
                            }}
                        >
                            Connect
                        </Button>
                    </>
                )}
            </Box>
        </>
    );
}