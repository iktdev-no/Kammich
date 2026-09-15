import { useEffect, useState } from "react";
import type { ChangeEvent } from "react";
import {
    Box,
    Typography,
    Button,
    List,
    Collapse,
    IconButton,
    Tooltip,
    Switch,
    Divider,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import EthernetIcon from "@mui/icons-material/SettingsEthernet";
import CableIcon from "@mui/icons-material/Cable";
import RefreshIcon from "@mui/icons-material/Refresh";
import PowerIcon from "@mui/icons-material/Power";
import RouterIcon from "@mui/icons-material/Router";

import { useSseSelector } from "../../sse/useSseSelector";
import type { EthernetConnection, EthernetConnectionStateType } from "../../types/types";
import { ethernetApi } from "../../api/requests/networking/ethernet";
import { useCountdown } from "../../hooks/useCountdown";

export default function Ethernet() {
    const [expandedInterface, setExpandedInterface] = useState<string | false>(false);
    const [localConnections, setLocalConnections] = useState<EthernetConnection[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);

    const connectionsFromSse = useSseSelector(
        state => state.ethConnection
    );

    useEffect(() => {
        void loadConnections();
    }, []);

    useEffect(() => {
        if (connectionsFromSse) {
            const connections: EthernetConnection[] = Object.values(connectionsFromSse);

            setLocalConnections(connections);
            setIsLoading(false);

            if (connections.length > 0) {
                setExpandedInterface(prev =>
                    prev === false ? connections[0].ifName : prev
                );
            }
        }
    }, [connectionsFromSse]);

    async function loadConnections(): Promise<void> {
        try {
            const data: EthernetConnection[] = await ethernetApi.getAll();

            setLocalConnections(data);

            if (data.length > 0) {
                setExpandedInterface(data[0].ifName);
            }
        } catch (error: unknown) {
            console.error("Klarte ikke å hente Ethernet-grensesnitt:", error);
        } finally {
            setIsLoading(false);
        }
    }

    function toggleInterface(interfaceName: string): void {
        setExpandedInterface(prev =>
            prev === interfaceName ? false : interfaceName
        );
    }

    return (
        <Box sx={{
            maxWidth: "800px",
            mx: "auto",
            p: 4,
            display: "flex",
            flexDirection: "column",
            gap: 3
        }}>
            <Box sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between"
            }}>
                <Typography variant="h5" sx={{ fontWeight: 600 }}>
                    Ethernet Settings
                </Typography>

                <Tooltip title="Refresh" arrow>
                    <IconButton
                        onClick={() => void loadConnections()}
                        disabled={isLoading}
                    >
                        <RefreshIcon />
                    </IconButton>
                </Tooltip>
            </Box>

            {localConnections.map((connection: EthernetConnection) => (
                <EthernetCard
                    key={connection.ifName}
                    connection={connection}
                    expanded={expandedInterface === connection.ifName}
                    onToggle={() => toggleInterface(connection.ifName)}
                />
            ))}

            {localConnections.length === 0 && !isLoading && (
                <Box sx={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    p: 4,
                    color: "text.secondary"
                }}>
                    <EthernetIcon sx={{ mt: 8, fontSize: 72 }} />

                    <Typography sx={{ mt: 4 }}>
                        No Ethernet interfaces found
                    </Typography>
                </Box>
            )}
        </Box>
    );
}

interface EthernetCardProps {
    connection: EthernetConnection;
    expanded: boolean;
    onToggle: () => void;
}

function EthernetCard({
    connection,
    expanded,
    onToggle,
}: EthernetCardProps) {
    const { ifName, state, carrier } = connection;

    return (
        <Box sx={{
            bgcolor: "grey.900",
            border: 1,
            borderColor: "grey.900",
            borderRadius: 2.5,
            overflow: "hidden"
        }}>
            <Box sx={{
                p: 2,
                display: "flex",
                alignItems: "center",
                borderBottom: expanded
                    ? "1px solid rgba(255,255,255,0.05)"
                    : "none"
            }}>
                <Box sx={{
                    display: "flex",
                    alignItems: "center",
                    flexGrow: 1,
                    minWidth: 0
                }}>
                    <EthernetIcon sx={{
                        mr: 1.5,
                        color: carrier ? "success.main" : "text.secondary"
                    }} />

                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Typography sx={{ fontWeight: 600 }}>
                            {ifName}
                        </Typography>

                        <Typography
                            variant="caption"
                            sx={{
                                color: getStateColor(state),
                                fontWeight: 500
                            }}
                        >
                            {getStateLabel(state)}
                        </Typography>
                    </Box>

                    <Tooltip title={expanded ? "Lukk" : "Åpne"} arrow>
                        <IconButton onClick={onToggle}>
                            <ExpandMoreIcon sx={{
                                transform: expanded
                                    ? "rotate(180deg)"
                                    : "rotate(0deg)",
                                transition: "transform 0.2s ease-in-out",
                                color: "text.secondary"
                            }} />
                        </IconButton>
                    </Tooltip>
                </Box>
            </Box>

            <Collapse in={expanded} timeout="auto" unmountOnExit>
                <Box sx={{ p: 2 }}>
                    <EthernetInterfaceContent connection={connection} />
                </Box>
            </Collapse>
        </Box>
    );
}

interface EthernetInterfaceContentProps {
    connection: EthernetConnection;
}

function EthernetInterfaceContent({
    connection,
}: EthernetInterfaceContentProps) {
    const [busy, setBusy] = useState<boolean>(false);

    const {
        ifName,
        state,
        carrier,
        ipv4,
        emergencyAllowed,
        emergencyAt,
    } = connection;

    async function runAction(action: () => Promise<unknown>): Promise<void> {
        if (busy) {
            return;
        }

        setBusy(true);

        try {
            await action();
        } catch (error: unknown) {
            console.error(`Ethernet action failed for ${ifName}: `, error);
        } finally {
            setBusy(false);
        }
    }

    function handleClient(): void {
        void runAction(() => ethernetApi.startClient(ifName));
    }

    function handleTether(): void {
        void runAction(() => ethernetApi.startTether(ifName));
    }

    function handleDisconnect(): void {
        void runAction(() => ethernetApi.disconnect(ifName));
    }

    function handleReset(): void {
        void runAction(() => ethernetApi.reset(ifName));
    }

    function handleEmergencyChanged(
        event: ChangeEvent<HTMLInputElement>
    ): void {
        const enabled: boolean = event.target.checked;

        void runAction(() =>
            ethernetApi.setEmergency(ifName, enabled)
        );
    }

    return (
        <Box sx={{
            display: "flex",
            flexDirection: "column",
            gap: 2.5
        }}>
            <EthernetStatus
                state={state}
                carrier={carrier}
                ipv4={ipv4}
                emergencyAt={emergencyAt}
            />

            <Divider sx={{ opacity: 0.1 }} />

            <EthernetActions
                state={state}
                busy={busy}
                emergencyAllowed={emergencyAllowed}
                onClient={handleClient}
                onTether={handleTether}
                onDisconnect={handleDisconnect}
            />

            <Divider sx={{ opacity: 0.1 }} />

            <EmergencySetting
                enabled={emergencyAllowed}
                disabled={busy}
                onChange={handleEmergencyChanged}
            />
        </Box>
    );
}

interface EthernetStatusProps {
    state: EthernetConnectionStateType;
    carrier: boolean;
    ipv4?: string | null;
    emergencyAt?: string | null;
}

function EthernetStatus({
    state,
    carrier,
    ipv4,
    emergencyAt,
}: EthernetStatusProps) {
    const emergencyCountdown: number | null = useCountdown(
        state === "Connecting" ? emergencyAt ?? null : null
    );

    return (
        <List disablePadding sx={{
            display: "flex",
            flexDirection: "column",
            gap: 1
        }}>
            <StatusRow
                icon={<CableIcon />}
                label="Cable"
                value={carrier ? "Connected" : "Disconnected"}
                color={carrier ? "success.main" : "text.secondary"}
            />

            <StatusRow
                icon={<PowerIcon />}
                label="State"
                value={getStateLabel(state)}
                color={getStateColor(state)}
            />

            <StatusRow
                icon={<RouterIcon />}
                label="IPv4"
                value={ipv4 ?? "None"}
                color={ipv4 ? "success.main" : "text.secondary"}
            />

            {emergencyCountdown !== null && (
                <StatusRow
                    icon={<RouterIcon />}
                    label="Emergency fallback"
                    value={`${emergencyCountdown}s`}
                    color="warning.main"
                />
            )}
        </List>
    );
}

function formatCountdown(emergencyAt: string): string {
    const remaining: number = Math.max(
        0,
        Math.ceil((new Date(emergencyAt).getTime() - Date.now()) / 1000)
    );

    return `${remaining}s`;
}

interface StatusRowProps {
    icon: React.ReactNode;
    label: string;
    value: string;
    color?: string;
}

function StatusRow({
    icon,
    label,
    value,
    color = "text.primary",
}: StatusRowProps) {
    return (
        <Box sx={{
            display: "flex",
            alignItems: "center",
            gap: 1.5,
            px: 1,
            py: 0.5
        }}>
            <Box sx={{
                display: "flex",
                color: "text.secondary"
            }}>
                {icon}
            </Box>

            <Typography
                variant="body2"
                sx={{
                    flexGrow: 1,
                    color: "text.secondary"
                }}
            >
                {label}
            </Typography>

            <Typography
                variant="body2"
                sx={{
                    color,
                    fontWeight: 500
                }}
            >
                {value}
            </Typography>
        </Box>
    );
}

interface EthernetActionsProps {
    state: EthernetConnectionStateType;
    busy: boolean;
    emergencyAllowed: boolean;
    onClient: () => void;
    onTether: () => void;
    onDisconnect: () => void;
}

function EthernetActions({
    state,
    busy,
    emergencyAllowed,
    onClient,
    onTether,
    onDisconnect,
}: EthernetActionsProps) {
    const isConnected: boolean = state === "Connected";
    const isEmergency: boolean = state === "Emergency";
    const isConnecting: boolean = state === "Connecting";

    return (
        <Box sx={{
            display: "flex",
            flexWrap: "wrap",
            gap: 1
        }}>
            <Button
                size="small"
                variant={isConnected || isConnecting ? "contained" : "outlined"}
                onClick={onClient}
                disabled={busy || (isEmergency && !emergencyAllowed)}
            >
                Client
            </Button>

            <Button
                size="small"
                variant={isEmergency ? "contained" : "outlined"}
                onClick={onTether}
                disabled={busy}
            >
                Emergency
            </Button>

            <Button
                size="small"
                variant="outlined"
                onClick={onDisconnect}
                disabled={busy || state === "Disconnected"}
            >
                Disconnect
            </Button>

        </Box>
    );
}

interface EmergencySettingProps {
    enabled: boolean;
    disabled: boolean;
    onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}

function EmergencySetting({
    enabled,
    disabled,
    onChange,
}: EmergencySettingProps) {
    return (
        <Box sx={{
            display: "flex",
            alignItems: "center",
            px: 1
        }}>
            <Box sx={{ flexGrow: 1 }}>
                <Typography variant="body2">
                    Emergency fallback
                </Typography>

                <Typography
                    variant="caption"
                    sx={{ color: "text.secondary" }}
                >
                    Automatically enable emergency mode if Client cannot
                    obtain an IPv4 address.
                </Typography>
            </Box>

            <Switch
                checked={enabled}
                disabled={disabled}
                onChange={onChange}
            />
        </Box>
    );
}

function getStateLabel(state: EthernetConnectionStateType): string {
    switch (state) {
        case "Connected":
            return "Connected";
        case "Connecting":
            return "Connecting";
        case "Emergency":
            return "Emergency";
        case "Disconnected":
            return "Disconnected";
        case "Idle":
            return "Idle";
    }
}

function getStateColor(state: EthernetConnectionStateType): string {
    switch (state) {
        case "Connected":
            return "success.main";
        case "Connecting":
            return "warning.main";
        case "Emergency":
            return "warning.main";
        case "Disconnected":
            return "error.main";
        case "Idle":
            return "text.secondary";
    }
}