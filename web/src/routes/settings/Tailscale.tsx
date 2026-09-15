import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
    Box,
    Typography,
    IconButton,
    Tooltip,
    CircularProgress,
    Button,
    Divider,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import TailscaleIcon from "@mui/icons-material/VpnKey";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorIcon from "@mui/icons-material/Error";
import NetworkCheckIcon from "@mui/icons-material/NetworkCheck";
import DnsIcon from "@mui/icons-material/Dns";
import LanIcon from "@mui/icons-material/Lan";
import RouterIcon from "@mui/icons-material/Router";

import { tailscaleApi } from "../../api/requests/networking/tailscale";
import type {
    TailscaleDns,
    TailscaleNetcheck,
    TailscaleServe,
    TailscaleStatus,
} from "../../types/types";

export default function Tailscale() {
    const [installed, setInstalled] = useState<boolean>(false);
    const [status, setStatus] = useState<TailscaleStatus | null>(null);
    const [serve, setServe] = useState<TailscaleServe[]>([]);
    const [netcheck, setNetcheck] = useState<TailscaleNetcheck | null>(null);
    const [dns, setDns] = useState<TailscaleDns | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [isNetchecking, setIsNetchecking] = useState<boolean>(false);

    useEffect(() => {
        void loadTailscale();
    }, []);

    async function loadTailscale(): Promise<void> {
        setIsLoading(true);

        try {
            const installed: boolean = await tailscaleApi.isInstalled();
            setInstalled(installed);

            if (!installed) {
                setStatus(null);
                setServe([]);
                setDns(null);
                setNetcheck(null);
                return;
            }

            const [
                tailscaleStatus,
                tailscaleServe,
                tailscaleDns,
            ] = await Promise.all([
                tailscaleApi.getStatus(),
                tailscaleApi.getServe(),
                tailscaleApi.getDns(),
            ]);

            setStatus(tailscaleStatus);
            setServe(tailscaleServe);
            setDns(tailscaleDns);
        } catch (error: unknown) {
            console.error("Klarte ikke å hente Tailscale-status:", error);
        } finally {
            setIsLoading(false);
        }
    }

    async function runNetcheck(): Promise<void> {
        if (isNetchecking) {
            return;
        }

        setIsNetchecking(true);

        try {
            const result: TailscaleNetcheck | null =
                await tailscaleApi.getNetcheck();

            setNetcheck(result);
        } catch (error: unknown) {
            console.error("Netcheck feilet:", error);
        } finally {
            setIsNetchecking(false);
        }
    }

    return (
        <Box sx={{
            maxWidth: "800px",
            mx: "auto",
            p: 4,
            display: "flex",
            flexDirection: "column",
            gap: 2,
        }}>
            <Box sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
            }}>
                <Typography variant="h5" sx={{ fontWeight: 600 }}>
                    Tailscale
                </Typography>

                <Tooltip title="Refresh" arrow>
                    <IconButton
                        onClick={() => void loadTailscale()}
                        disabled={isLoading}
                    >
                        <RefreshIcon />
                    </IconButton>
                </Tooltip>
            </Box>

            {isLoading ? (
                <LoadingState />
            ) : !installed ? (
                <NotInstalled />
            ) : (
                <Box sx={{
                    display: "flex",
                    flexDirection: "column",
                    gap: 2,
                }}>
                    <TailscaleStatusCard status={status} />

                    <TailscaleNetworkCard
                        netcheck={netcheck}
                        isNetchecking={isNetchecking}
                        onRunNetcheck={() => void runNetcheck()}
                    />

                    <TailscaleDnsCard dns={dns} />

                    <TailscaleServeCard serve={serve} />
                </Box>
            )}
        </Box>
    );
}

interface TailscaleStatusCardProps {
    status: TailscaleStatus | null;
}

function TailscaleStatusCard({
    status,
}: TailscaleStatusCardProps) {
    const online: boolean = status?.self.online === true;

    return (
        <TailscaleCard
            icon={<TailscaleIcon />}
            title="Tailscale"
            subtitle={online ? "Online" : "Offline"}
            statusIcon={
                online
                    ? <CheckCircleIcon sx={{ color: "success.main" }} />
                    : <ErrorIcon sx={{ color: "warning.main" }} />
            }
        >
            <Divider sx={{ opacity: 0.1 }} />

            <Box sx={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 1,
                p: 2,
            }}>
                <StatusItem
                    label="Hostname"
                    value={status?.self.dnsName ?? "-"}
                />
                <StatusItem
                    label="Backend"
                    value={status?.backendState ?? "-"}
                />
                <StatusItem
                    label="Relay"
                    value={status?.self.relay ?? "-"}
                />
                <StatusItem
                    label="Version"
                    value={status?.version ?? "-"}
                />
                <StatusItem
                    label="TUN"
                    value={status?.tun ? "Enabled" : "Disabled"}
                    good={status?.tun}
                />
                <StatusItem
                    label="Exit node"
                    value={status?.self.exitNode ? "Yes" : "No"}
                />
            </Box>
        </TailscaleCard>
    );
}

interface TailscaleNetworkCardProps {
    netcheck: TailscaleNetcheck | null;
    isNetchecking: boolean;
    onRunNetcheck: () => void;
}

function TailscaleNetworkCard({
    netcheck,
    isNetchecking,
    onRunNetcheck,
}: TailscaleNetworkCardProps) {
    return (
        <TailscaleCard
            icon={<NetworkCheckIcon />}
            title="Network check"
            subtitle={
                netcheck
                    ? "Last check completed"
                    : "Check Tailscale network connectivity"
            }
        >
            {!netcheck ? (
                <Box sx={{
                    p: 2,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    gap: 2,
                }}>
                    <Typography
                        variant="body2"
                        sx={{ color: "text.secondary" }}
                    >
                        Check UDP, IPv4, IPv6 and the preferred DERP relay.
                    </Typography>

                    <Button
                        variant="outlined"
                        startIcon={
                            isNetchecking
                                ? <CircularProgress size={16} />
                                : <NetworkCheckIcon />
                        }
                        onClick={onRunNetcheck}
                        disabled={isNetchecking}
                    >
                        {isNetchecking ? "Checking..." : "Run netcheck"}
                    </Button>
                </Box>
            ) : (
                <>
                    <Divider sx={{ opacity: 0.1 }} />

                    <Box sx={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: 1,
                        p: 2,
                    }}>
                        <StatusItem
                            label="UDP"
                            value={netcheck.udp ? "Available" : "Unavailable"}
                            good={netcheck.udp}
                        />
                        <StatusItem
                            label="IPv4"
                            value={netcheck.ipv4 ? "Available" : "Unavailable"}
                            good={netcheck.ipv4}
                        />
                        <StatusItem
                            label="IPv4 can send"
                            value={netcheck.ipv4CanSend ? "Yes" : "No"}
                            good={netcheck.ipv4CanSend}
                        />
                        <StatusItem
                            label="IPv6"
                            value={netcheck.ipv6 ? "Available" : "Unavailable"}
                            good={netcheck.ipv6}
                        />
                        <StatusItem
                            label="IPv6 can send"
                            value={netcheck.ipv6CanSend ? "Yes" : "No"}
                            good={netcheck.ipv6CanSend}
                        />
                        <StatusItem
                            label="ICMPv4"
                            value={netcheck.icmpv4 ? "Available" : "Unavailable"}
                            good={netcheck.icmpv4}
                        />
                        <StatusItem
                            label="Preferred DERP"
                            value={netcheck.preferredDerp?.toString() ?? "-"}
                        />
                        <StatusItem
                            label="Global IPv4"
                            value={netcheck.globalV4 ?? "-"}
                        />
                        <StatusItem
                            label="Global IPv6"
                            value={netcheck.globalV6 ?? "-"}
                        />
                        <StatusItem
                            label="Captive portal"
                            value={netcheck.captivePortal ?? "None"}
                        />
                    </Box>

                    <Box sx={{
                        px: 2,
                        pb: 2,
                        display: "flex",
                        justifyContent: "flex-end",
                    }}>
                        <Button
                            size="small"
                            startIcon={
                                isNetchecking
                                    ? <CircularProgress size={14} />
                                    : <RefreshIcon />
                            }
                            onClick={onRunNetcheck}
                            disabled={isNetchecking}
                        >
                            Run again
                        </Button>
                    </Box>
                </>
            )}
        </TailscaleCard>
    );
}

interface TailscaleDnsCardProps {
    dns: TailscaleDns | null;
}

function TailscaleDnsCard({
    dns,
}: TailscaleDnsCardProps) {
    return (
        <TailscaleCard
            icon={<DnsIcon />}
            title="DNS"
            subtitle={dns?.magicDnsEnabled ? "MagicDNS enabled" : "DNS configuration"}
        >
            {!dns ? (
                <EmptyState text="No DNS information available" />
            ) : (
                <>
                    <Divider sx={{ opacity: 0.1 }} />

                    <Box sx={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: 1,
                        p: 2,
                    }}>
                        <StatusItem
                            label="MagicDNS"
                            value={dns.magicDnsEnabled ? "Enabled" : "Disabled"}
                            good={dns.magicDnsEnabled}
                        />
                        <StatusItem
                            label="Tailscale DNS"
                            value={dns.tailscaleDns ? "Enabled" : "Disabled"}
                            good={dns.tailscaleDns}
                        />
                        <StatusItem
                            label="DNS name"
                            value={dns.selfDnsName ?? "-"}
                        />
                        <StatusItem
                            label="MagicDNS suffix"
                            value={dns.magicDnsSuffix ?? "-"}
                        />
                        <StatusItem
                            label="Search domains"
                            value={dns.searchDomains.length > 0
                                ? dns.searchDomains.join(", ")
                                : "-"}
                        />
                        <StatusItem
                            label="Certificates"
                            value={dns.certDomains.length > 0
                                ? dns.certDomains.join(", ")
                                : "-"}
                        />
                    </Box>
                </>
            )}
        </TailscaleCard>
    );
}

interface TailscaleServeCardProps {
    serve: TailscaleServe[];
}

function TailscaleServeCard({
    serve,
}: TailscaleServeCardProps) {
    return (
        <TailscaleCard
            icon={<LanIcon />}
            title="Serve"
            subtitle={
                serve.length === 0
                    ? "No services configured"
                    : `${serve.length} service${serve.length === 1 ? "" : "s"}`
            }
        >
            {serve.length === 0 ? (
                <EmptyState text="No services are being served" />
            ) : (
                <>
                    <Divider sx={{ opacity: 0.1 }} />

                    <Box sx={{
                        display: "flex",
                        flexDirection: "column",
                        p: 1,
                    }}>
                        {serve.map((entry: TailscaleServe, index: number) => (
                            <Box
                                key={`${entry.hostname}-${entry.port}-${index}`}
                                sx={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: 1.5,
                                    px: 1,
                                    py: 1,
                                }}
                            >
                                <RouterIcon
                                    sx={{
                                        fontSize: 20,
                                        color: "text.secondary",
                                    }}
                                />

                                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                    <Typography
                                        variant="body2"
                                        sx={{ fontWeight: 500 }}
                                    >
                                        {entry.hostname}
                                    </Typography>

                                    <Typography
                                        variant="caption"
                                        sx={{ color: "text.secondary" }}
                                    >
                                        {entry.path || "/"} → {entry.proxy}
                                    </Typography>
                                </Box>

                                <Typography
                                    variant="caption"
                                    sx={{
                                        color: "text.secondary",
                                        whiteSpace: "nowrap",
                                    }}
                                >
                                    {entry.https ? "HTTPS" : "HTTP"}:{entry.port}
                                </Typography>
                            </Box>
                        ))}
                    </Box>
                </>
            )}
        </TailscaleCard>
    );
}

interface TailscaleCardProps {
    icon: ReactNode;
    title: string;
    subtitle: string;
    statusIcon?: ReactNode;
    children: ReactNode;
}

function TailscaleCard({
    icon,
    title,
    subtitle,
    statusIcon,
    children,
}: TailscaleCardProps) {
    return (
        <Box sx={{
            bgcolor: "grey.900",
            borderRadius: 2.5,
            overflow: "hidden",
        }}>
            <Box sx={{
                display: "flex",
                alignItems: "center",
                gap: 1.5,
                p: 2,
            }}>
                <Box sx={{
                    display: "flex",
                    alignItems: "center",
                    color: "text.secondary",
                }}>
                    {icon}
                </Box>

                <Box sx={{ flexGrow: 1 }}>
                    <Typography sx={{ fontWeight: 600 }}>
                        {title}
                    </Typography>

                    <Typography
                        variant="caption"
                        sx={{ color: "text.secondary" }}
                    >
                        {subtitle}
                    </Typography>
                </Box>

                {statusIcon}
            </Box>

            {children}
        </Box>
    );
}

interface StatusItemProps {
    label: string;
    value: string;
    good?: boolean;
}

function StatusItem({
    label,
    value,
    good,
}: StatusItemProps) {
    return (
        <Box sx={{
            display: "flex",
            flexDirection: "column",
            gap: 0.25,
            px: 1,
            py: 0.75,
            minWidth: 0,
        }}>
            <Typography
                variant="caption"
                sx={{ color: "text.secondary" }}
            >
                {label}
            </Typography>

            <Typography
                variant="body2"
                sx={{
                    fontWeight: 500,
                    color: good === undefined
                        ? "text.primary"
                        : good
                            ? "success.main"
                            : "error.main",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                }}
            >
                {value}
            </Typography>
        </Box>
    );
}

interface EmptyStateProps {
    text: string;
}

function EmptyState({
    text,
}: EmptyStateProps) {
    return (
        <Typography
            variant="body2"
            sx={{
                px: 2,
                py: 2,
                color: "text.secondary",
            }}
        >
            {text}
        </Typography>
    );
}

function LoadingState() {
    return (
        <Box sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            p: 5,
        }}>
            <CircularProgress size={28} />
        </Box>
    );
}

function NotInstalled() {
    return (
        <Box sx={{
            bgcolor: "grey.900",
            borderRadius: 2.5,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            p: 5,
            color: "text.secondary",
        }}>
            <TailscaleIcon sx={{ fontSize: 56 }} />

            <Typography sx={{ mt: 2 }}>
                Tailscale is not installed
            </Typography>
        </Box>
    );
}